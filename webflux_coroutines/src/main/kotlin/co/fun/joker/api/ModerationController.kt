package co.`fun`.joker.api

import co.`fun`.joker.repository.Moderation
import co.`fun`.joker.repository.ModerationRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.mono
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
    fun moderate(@RequestBody request: ModerationRequest): Mono<ModerationResponse> = mono {
        val contentType = contentTypes.random()

        val moderationAsync = async { moderationRepository.findById(request.id) }

        val textAsync = async { recognizeText(request, contentType) }
        val textClassifyResultAsync = async {
            textAsync.await().text?.let { text -> classifyText(TextClassifyRequest(request.id, text)) }
                ?: ModerationPartialResponse("", Decision.VALID)
        }

        val classify = when (contentType) {
            ContentType.IMAGE -> classifyContent(request, contentType)
            ContentType.VIDEO -> moderationAsync.await()
                ?.let { ModerationPartialResponse(it.id, it.decision) }
                ?: classifyContent(request, contentType)
        }

        val labels = async {
            classify.let {
                if (it.decision == Decision.NOT_SUITED)
                    collectLabels(ContentLabelRequest(request.id, request.url))
                else
                    LabelsResponse("", emptyList())
            }
        }

        val textClassify = textClassifyResultAsync.await()
        val decisions = listOfNotNull(classify.decision, textClassify.decision)
        val resultDecision = when {
            Decision.BANNED in decisions -> Decision.BANNED
            Decision.NOT_SUITED in decisions -> Decision.NOT_SUITED
            else -> Decision.VALID
        }

        ModerationResponse(
            request.id,
            resultDecision,
            classify.decision,
            textClassify.decision,
            contentType,
            textAsync.await().text,
            labels.await().labels
        ).also {
            moderationRepository.save(Moderation(request.id, it.finalDecision))
        }
    }

    private suspend fun recognizeText(request: ModerationRequest, contentType: ContentType): RecognizeResponse {
        return webClient.post()
            .uri("${Env.SERVICE}/text/recognize")
            .bodyValue(TextRecognizeRequest(request.id, request.url, contentType, request.additionalDelay))
            .retrieve()
            .bodyToMono(RecognizeResponse::class.java)
            .awaitFirst()
    }

    private suspend fun classifyText(classifyRequest: TextClassifyRequest): ModerationPartialResponse {
        return webClient.post()
            .uri( "${Env.SERVICE}/text/classify")
            .bodyValue(classifyRequest)
            .retrieve()
            .bodyToMono(ModerationPartialResponse::class.java)
            .awaitFirst()
    }

    private suspend fun classifyContent(request: ModerationRequest, contentType: ContentType): ModerationPartialResponse {
        return webClient.post()
            .uri( "${Env.SERVICE}/content/classify")
            .bodyValue(ContentClassifyRequest(request.id, request.url, contentType, request.additionalDelay))
            .retrieve()
            .bodyToMono(ModerationPartialResponse::class.java)
            .awaitFirst()
    }

    private suspend fun collectLabels(request: ContentLabelRequest): LabelsResponse {
        return webClient.post()
            .uri( "${Env.SERVICE}/content/labels")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(LabelsResponse::class.java)
            .awaitFirst()
    }
}