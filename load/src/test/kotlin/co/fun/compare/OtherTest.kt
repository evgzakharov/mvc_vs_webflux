package co.`fun`.compare

import co.`fun`.compare.api.ContentClassifyRequest
import co.`fun`.compare.api.ContentType
import co.`fun`.compare.api.Env.SERVICE
import co.`fun`.compare.api.ModerationPartialResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.post
import io.ktor.http.contentType
import org.junit.jupiter.api.Test

class OtherTest {
    private val ktorClientJava = HttpClient(Apache) {
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