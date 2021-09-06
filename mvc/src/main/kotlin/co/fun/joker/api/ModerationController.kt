package co.`fun`.joker.api

import co.`fun`.joker.repository.Moderation
import co.`fun`.joker.repository.ModerationRepository
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.CompletableFuture.supplyAsync
import java.util.concurrent.Executors

@RestController
@RequestMapping("/joker2021")
class ModerationController(
    restTemplateBuilder: RestTemplateBuilder,
    private val moderationRepository: ModerationRepository
) {
    private val restTemplate = restTemplateBuilder.build()
    private val pool = Executors.newCachedThreadPool()
    private val contentTypes = ContentType.values()

    @PostMapping("moderation")
    fun moderate(@RequestBody request: ModerationRequest): ModerationResponse {
        val contentType = contentTypes.random()

        val moderationAsync = supplyAsync({ moderationRepository.findById(request.id) }, pool)

        val textAsync = supplyAsync({ recognizeText(request, contentType) }, pool)
        val textClassifyResultAsync = textAsync.thenApplyAsync({
            it?.text?.let { text -> classifyText(TextClassifyRequest(request.id, text)) }
                ?: ModerationPartialResponse("", Decision.VALID)
        }, pool)

        val classifyAsync = when(contentType) {
            ContentType.IMAGE -> supplyAsync({ classifyContent(request, contentType) }, pool)
            ContentType.VIDEO -> moderationAsync.thenApplyAsync({
                if (it != null)
                    ModerationPartialResponse(it.id, it.decision)
                else
                    classifyContent(request, contentType)
            }, pool)
        }

        val labels = classifyAsync.thenApplyAsync({
            if (it?.decision == Decision.NOT_SUITED)
                collectLabels(ContentLabelRequest(request.id, request.url))
            else
                null
        }, pool)

        val resultDecision = textClassifyResultAsync.thenCombine(classifyAsync) { textClassify, classify ->
            val decisions = listOfNotNull(textClassify?.decision, classify?.decision)

            when {
                Decision.BANNED in decisions -> Decision.BANNED
                Decision.NOT_SUITED in decisions -> Decision.NOT_SUITED
                else -> Decision.VALID
            }
        }

        return ModerationResponse(
            request.id,
            resultDecision.get(),
            classifyAsync.get()!!.decision,
            textClassifyResultAsync.get()?.decision,
            contentType,
            textAsync.get()?.text,
            labels.get()?.labels ?: emptyList()
        ).also {
            moderationRepository.save(Moderation(request.id, resultDecision.get()))
        }
    }

    private fun recognizeText(request: ModerationRequest, contentType: ContentType): RecognizeResponse? {
        return restTemplate.postForEntity(
            "${Env.SERVICE}/text/recognize",
            TextRecognizeRequest(request.id, request.url, contentType, request.additionalDelay),
            RecognizeResponse::class.java
        ).body
    }

    private fun classifyText(classifyRequest: TextClassifyRequest): ModerationPartialResponse? {
        return restTemplate.postForEntity(
            "${Env.SERVICE}/text/classify",
            classifyRequest,
            ModerationPartialResponse::class.java
        ).body
    }

    private fun classifyContent(request: ModerationRequest, contentType: ContentType): ModerationPartialResponse? {
        return restTemplate.postForEntity(
            "${Env.SERVICE}/content/classify",
            ContentClassifyRequest(request.id, request.url, contentType, request.additionalDelay),
            ModerationPartialResponse::class.java
        ).body
    }

    private fun collectLabels(request: ContentLabelRequest): LabelsResponse? {
        return restTemplate.postForEntity(
            "${Env.SERVICE}/content/labels",
            request,
            LabelsResponse::class.java
        ).body
    }
}