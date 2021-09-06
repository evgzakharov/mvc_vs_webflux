package co.`fun`.joker

import co.`fun`.joker.api.ContentClassifyRequest
import co.`fun`.joker.api.ContentType
import co.`fun`.joker.api.Env.SERVICE
import co.`fun`.joker.api.ModerationPartialResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.post
import io.ktor.http.contentType
import org.junit.jupiter.api.Test

class OtherTest {
    private val ktorClientJava = HttpClient(Java) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }

    @Test
    fun `test response`() = runBlockingWithDefersU {
        val response = runCatching {
            ktorClientJava.post<ModerationPartialResponse>("$SERVICE/image/classify") {
                contentType(io.ktor.http.ContentType.Application.Json)
                body = ContentClassifyRequest("123", "test", ContentType.IMAGE)
            }
        }
        println(response)
    }

}