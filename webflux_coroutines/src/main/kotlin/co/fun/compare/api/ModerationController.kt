package co.`fun`.compare.api

import co.`fun`.compare.repository.Moderation
import co.`fun`.compare.repository.ModerationRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider


@RestController
@RequestMapping("/compare")
class ModerationController(
    webClientBuilder: WebClient.Builder,
    private val moderationRepository: ModerationRepository
) {
    private val webClient = run {
        val connectionProvider = ConnectionProvider.builder("myConnectionPool")
            .maxConnections(1_000)
            .pendingAcquireMaxCount(10_000)
            .build()
        val clientHttpConnector = ReactorClientHttpConnector(HttpClient.create(connectionProvider))

        webClientBuilder
            .clientConnector(clientHttpConnector)
            .build()
    }
    private val contentTypes = ContentType.values()

    @PostMapping("moderation")
    suspend fun moderate(@RequestBody request: ModerationRequest): ModerationResponse = coroutineScope {
        val contentType = contentTypes.random()

        val moderationAsync = async { findInDb(request) }

        val textAsync = async { recognizeText(request, contentType) }
        val textClassifyResultAsync = async {
            textAsync.await().text?.let { text -> classifyText(request, text) }
                ?: ModerationPartialResponse("", Decision.VALID)
        }

        val classify = when (contentType) {
            ContentType.IMAGE -> classifyContent(request, contentType)
            ContentType.VIDEO -> moderationAsync.await()
                ?.let { ModerationPartialResponse(it.id, it.decision) }
                ?: classifyContent(request, contentType)
        }

        val labels = async {
            if (classify.decision == Decision.NOT_SUITED)
                collectLabels(request)
            else
                LabelsResponse("", emptyList())
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
            saveToDb(request, it)
        }
    }

    private suspend fun recognizeText(request: ModerationRequest, contentType: ContentType): RecognizeResponse {
        val textRecognizeRequest = TextRecognizeRequest(request.id, request.url, contentType, request.additionalDelay)

        if (request.mockCalls)
            return  ServerMock.recognizeText(textRecognizeRequest)

        return webClient.post()
            .uri("${Env.SERVICE}/text/recognize")
            .bodyValue(textRecognizeRequest)
            .retrieve()
            .bodyToMono(RecognizeResponse::class.java)
            .awaitFirst()
    }

    private suspend fun classifyText(request: ModerationRequest, text: String): ModerationPartialResponse {
        val textRequest = TextClassifyRequest(request.id, text, request.additionalDelay)

        if (request.mockCalls)
            return  ServerMock.classifyText(textRequest)

        return webClient.post()
            .uri( "${Env.SERVICE}/text/classify")
            .bodyValue(textRequest)
            .retrieve()
            .bodyToMono(ModerationPartialResponse::class.java)
            .awaitFirst()
    }

    private suspend fun classifyContent(request: ModerationRequest, contentType: ContentType): ModerationPartialResponse {
        val contentClassifyRequest = ContentClassifyRequest(request.id, request.url, contentType, request.additionalDelay)

        if (request.mockCalls)
            return  ServerMock.classifyContent(contentClassifyRequest)

        return webClient.post()
            .uri( "${Env.SERVICE}/content/classify")
            .bodyValue(contentClassifyRequest)
            .retrieve()
            .bodyToMono(ModerationPartialResponse::class.java)
            .awaitFirst()
    }

    private suspend fun collectLabels(request: ModerationRequest): LabelsResponse {
        val labelsRequest = ContentLabelRequest(request.id, request.url, request.additionalDelay)

        if (request.mockCalls)
            return  ServerMock.contentLabels(labelsRequest)

        return webClient.post()
            .uri( "${Env.SERVICE}/content/labels")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(LabelsResponse::class.java)
            .awaitFirst()
    }

    private suspend fun findInDb(request: ModerationRequest): Moderation? {
        if (request.mockCalls)
            return null

        return moderationRepository.findById(request.id)
    }

    private suspend fun saveToDb(request: ModerationRequest, it: ModerationResponse) {
        if (request.mockCalls)
            return

        moderationRepository.save(Moderation(request.id, it.finalDecision))
    }
}