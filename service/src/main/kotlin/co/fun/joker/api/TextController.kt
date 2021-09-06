package co.`fun`.joker.api

import kotlinx.coroutines.delay
import org.bson.types.ObjectId
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.random.Random

@RestController
@RequestMapping("text")
class TextController {
    private val possibleValues = Decision.values()

    @PostMapping("classify")
    suspend fun classifyImage(@RequestBody request: TextClassifyRequest): ModerationPartialResponse {
        request.additionalDelay?.let { delay(it) }

        return ModerationPartialResponse(
            ObjectId().toHexString(),
            possibleValues.random()
        )
    }

    @PostMapping("recognize")
    suspend fun recognize(@RequestBody request: TextRecognizeRequest): RecognizeResponse {
        request.additionalDelay?.let { delay(it) }

        return RecognizeResponse(
            ObjectId().toHexString(),
            Random.nextLong().toString()
        )
    }
}