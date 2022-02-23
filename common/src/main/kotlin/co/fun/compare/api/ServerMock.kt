package co.`fun`.compare.api

import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.random.Random

object ServerMock {
    private val possibleValues = Decision.values()

    suspend fun classifyContent(request: ContentClassifyRequest): ModerationPartialResponse {
        request.additionalDelay?.let { delay(it) }

        return ModerationPartialResponse(
            UUID.randomUUID().toString(),
            possibleValues.random()
        )
    }

    suspend fun contentLabels(request: ContentLabelRequest): LabelsResponse {
        request.additionalDelay?.let { delay(it) }

        return LabelsResponse(
            UUID.randomUUID().toString(),
            Random.nextLong().toString().map { it.toString() }
        )
    }

    suspend fun classifyText(request: TextClassifyRequest): ModerationPartialResponse {
        request.additionalDelay?.let { delay(it) }

        return ModerationPartialResponse(
            UUID.randomUUID().toString(),
            possibleValues.random()
        )
    }

    suspend fun recognizeText(request: TextRecognizeRequest): RecognizeResponse {
        request.additionalDelay?.let { delay(it) }

        return RecognizeResponse(
            UUID.randomUUID().toString(),
            Random.nextLong().toString()
        )
    }

}