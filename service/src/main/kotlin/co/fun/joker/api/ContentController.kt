package co.`fun`.joker.api

import kotlinx.coroutines.delay
import org.bson.types.ObjectId
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.random.Random

@RestController
@RequestMapping("content")
class ImageController {
    private val possibleValues = Decision.values()

    @PostMapping("classify")
    suspend fun classifyImage(@RequestBody request: ContentClassifyRequest): ModerationPartialResponse {
        request.additionalDelay?.let { delay(it) }

        return ModerationPartialResponse(
            ObjectId().toHexString(),
            possibleValues.random()
        )
    }

    @PostMapping("labels")
    suspend fun labels(@RequestBody request: ContentLabelRequest): LabelsResponse {
        request.additionalDelay?.let { delay(it) }

        return LabelsResponse(
            ObjectId().toHexString(),
            Random.nextLong().toString().map { it.toString() }
        )
    }
}