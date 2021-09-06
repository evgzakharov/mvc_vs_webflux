package co.`fun`.joker

import co.`fun`.joker.api.ModerationRequest
import co.`fun`.joker.api.ModerationResponse
import co.`fun`.joker.util.DataCollector
import co.`fun`.joker.util.LoadTester
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Duration
import java.util.UUID
import kotlin.random.Random

val loadData = File("times/test.txt").apply { parentFile.mkdirs() }
val collectData = File("times/test.csv").apply { parentFile.mkdirs() }

class LoadTest {
    private val ktorClientJava = HttpClient(Java) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }

    private val simpleUrl = "http://localhost:8080/joker2021/simple"
    private val cpuLoadUrl = "http://localhost:8080/joker2021/cpu-load"
    private val chainUrl = "http://localhost:8080/joker2021/chain"
    private val moderationUrl = "http://localhost:8080/joker2021/moderation"

    @Test
    fun `test response`() = runBlockingWithDefersU {
        val response = request(cpuLoadUrl)
        println(response)
    }

    @Test
    fun `test moderation response`() = runBlockingWithDefersU {
        repeat(100) {
            val response = moderationRequest(moderationUrl)
            println(response)
        }
    }


    @Test
    fun `run io test`() = runBlockingWithDefersU {
        val steps: List<Pair<Long, Int>> = listOf(
            Duration.ofSeconds(0).toMillis() to 10,
            Duration.ofSeconds(10).toMillis() to 100,
            Duration.ofSeconds(20).toMillis() to 150,
            Duration.ofSeconds(30).toMillis() to 200,
            Duration.ofSeconds(40).toMillis() to 250,
            Duration.ofSeconds(50).toMillis() to 300,
            Duration.ofSeconds(61).toMillis() to 300,
        )

        val tester = LoadTester(loadData, steps)
        tester.loadTest { moderationRequest(moderationUrl) }

        DataCollector.collectData(collectData)
    }

    @Test
    fun `run cpu test`() = runBlockingWithDefersU {
        val steps: List<Pair<Long, Int>> = listOf(
            Duration.ofSeconds(0).toMillis() to 1,
            Duration.ofSeconds(50).toMillis() to 10,
            Duration.ofSeconds(60).toMillis() to 10,
            Duration.ofSeconds(61).toMillis() to 10,
        )

        val tester = LoadTester(loadData, steps)
        tester.loadTest { request(cpuLoadUrl) }

        DataCollector.collectData(collectData)
    }

    private suspend fun request(url: String): String = ktorClientJava.get(url)

    private suspend fun moderationRequest(moderationUrl: String): ModerationResponse {
        return ktorClientJava.post(moderationUrl) {
            contentType(ContentType.Application.Json)
            body = ModerationRequest(UUID.randomUUID().toString(), Random.nextLong().toString())
        }
    }
}