package co.`fun`.compare.api

import co.`fun`.compare.repository.Moderation
import co.`fun`.compare.repository.ModerationRepository
import kotlinx.coroutines.reactor.mono
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/compare")
class ModerationController(
    webClientBuilder: WebClient.Builder,
    private val moderationRepository: ModerationRepository
) {
    private val webClient = webClientBuilder.build()
    private val contentTypes = ContentType.values()

    @PostMapping("moderation")
    fun moderate(@RequestBody request: ModerationRequest): Mono<ModerationResponse> {
        val contentType = contentTypes.random()

        val moderationAsync = findInDb(request)

        val textAsync = recognizeText(request, contentType).cache()
        val textClassifyResultAsync = textAsync.flatMap {
            it?.text?.let { text -> classifyText(request, text) }
                ?: Mono.just(ModerationPartialResponse("", Decision.VALID))
        }

        val classifyAsync = when (contentType) {
            ContentType.IMAGE -> classifyContent(request, contentType)
            ContentType.VIDEO -> moderationAsync
                .map { ModerationPartialResponse(it.id, it.decision) }
                .switchIfEmpty(classifyContent(request, contentType))
        }.cache()

        val labelsAsync = classifyAsync.flatMap {
            if (it?.decision == Decision.NOT_SUITED)
                collectLabels(request)
            else
                Mono.just(LabelsResponse("", emptyList()))
        }

        val resultDecision = textClassifyResultAsync.zipWith(classifyAsync) { textClassify, classify ->
            val decisions = listOfNotNull(textClassify?.decision, classify?.decision)

            when {
                Decision.BANNED in decisions -> Decision.BANNED
                Decision.NOT_SUITED in decisions -> Decision.NOT_SUITED
                else -> Decision.VALID
            }
        }

        return Mono.zip(classifyAsync, textClassifyResultAsync, textAsync, labelsAsync, resultDecision)
            .map { zipResult ->
                val classify = zipResult.t1
                val textClassify = zipResult.t2
                val text = zipResult.t3
                val labels = zipResult.t4
                val decision = zipResult.t5

                ModerationResponse(
                    request.id,
                    decision,
                    classify.decision,
                    textClassify.decision,
                    contentType,
                    text.text,
                    labels.labels
                )
            }.flatMap { response ->
                savaToDb(request, response)
                    .map { response }
            }
    }

    private fun recognizeText(request: ModerationRequest, contentType: ContentType): Mono<RecognizeResponse> {
        val textRecognizeRequest = TextRecognizeRequest(request.id, request.url, contentType, request.additionalDelay)

        if (request.mockCalls)
            return mono { ServerMock.recognizeText(textRecognizeRequest) }

        return webClient.post()
            .uri("${Env.SERVICE}/text/recognize")
            .bodyValue(textRecognizeRequest)
            .retrieve()
            .bodyToMono(RecognizeResponse::class.java)
    }

    private fun classifyText(request: ModerationRequest, text: String): Mono<ModerationPartialResponse> {
        val classifyRequest = TextClassifyRequest(request.id, text, request.additionalDelay)

        if (request.mockCalls)
            return mono { ServerMock.classifyText(classifyRequest) }

        return webClient.post()
            .uri( "${Env.SERVICE}/text/classify")
            .bodyValue(classifyRequest)
            .retrieve()
            .bodyToMono(ModerationPartialResponse::class.java)
    }

    private fun classifyContent(request: ModerationRequest, contentType: ContentType): Mono<ModerationPartialResponse> {
        val contentClassifyRequest = ContentClassifyRequest(request.id, request.url, contentType, request.additionalDelay)

        if (request.mockCalls)
            return mono { ServerMock.classifyContent(contentClassifyRequest) }

        return webClient.post()
            .uri( "${Env.SERVICE}/content/classify")
            .bodyValue(contentClassifyRequest)
            .retrieve()
            .bodyToMono(ModerationPartialResponse::class.java)
    }

    private fun collectLabels(request: ModerationRequest): Mono<LabelsResponse> {
        val labelsRequest = ContentLabelRequest(request.id, request.url, request.additionalDelay)

        if (request.mockCalls)
            return mono { ServerMock.contentLabels(labelsRequest) }

        return webClient.post()
            .uri( "${Env.SERVICE}/content/labels")
            .bodyValue(labelsRequest)
            .retrieve()
            .bodyToMono(LabelsResponse::class.java)
    }

    private fun findInDb(request: ModerationRequest): Mono<Moderation> {
        if (request.mockCalls)
            return Mono.empty()

        return moderationRepository.findById(request.id)
    }

    private fun savaToDb(
        request: ModerationRequest,
        response: ModerationResponse
    ): Mono<Moderation> {
        if (request.mockCalls)
            return Mono.just(Moderation("", response.finalDecision))

        return moderationRepository.save(Moderation(request.id, response.finalDecision))
    }
}