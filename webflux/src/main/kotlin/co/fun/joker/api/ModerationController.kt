package co.`fun`.joker.api

import co.`fun`.joker.repository.Moderation
import co.`fun`.joker.repository.ModerationRepository
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/joker2021")
class ModerationController(
    webClientBuilder: WebClient.Builder,
    private val moderationRepository: ModerationRepository
) {
    private val webClient = webClientBuilder.build()
    private val contentTypes = ContentType.values()

    @PostMapping("moderation")
    fun moderate(@RequestBody request: ModerationRequest): Mono<ModerationResponse> {
        val contentType = contentTypes.random()

        val moderationAsync = moderationRepository.findById(request.id)

        val textAsync = recognizeText(request, contentType).cache()
        val textClassifyResultAsync = textAsync.flatMap {
            it?.text?.let { text -> classifyText(TextClassifyRequest(request.id, text, request.additionalDelay)) }
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
                collectLabels(ContentLabelRequest(request.id, request.url, request.additionalDelay))
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
                moderationRepository.save(Moderation(request.id, response.finalDecision))
                    .map { response }
            }
    }

    private fun recognizeText(request: ModerationRequest, contentType: ContentType): Mono<RecognizeResponse> {
        return webClient.post()
            .uri("${Env.SERVICE}/text/recognize")
            .bodyValue(TextRecognizeRequest(request.id, request.url, contentType, request.additionalDelay))
            .retrieve()
            .bodyToMono(RecognizeResponse::class.java)
    }

    private fun classifyText(classifyRequest: TextClassifyRequest): Mono<ModerationPartialResponse> {
        return webClient.post()
            .uri( "${Env.SERVICE}/text/classify")
            .bodyValue(classifyRequest)
            .retrieve()
            .bodyToMono(ModerationPartialResponse::class.java)
    }

    private fun classifyContent(request: ModerationRequest, contentType: ContentType): Mono<ModerationPartialResponse> {
        return webClient.post()
            .uri( "${Env.SERVICE}/content/classify")
            .bodyValue(ContentClassifyRequest(request.id, request.url, contentType, request.additionalDelay))
            .retrieve()
            .bodyToMono(ModerationPartialResponse::class.java)
    }

    private fun collectLabels(request: ContentLabelRequest): Mono<LabelsResponse> {
        return webClient.post()
            .uri( "${Env.SERVICE}/content/labels")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(LabelsResponse::class.java)
    }
}