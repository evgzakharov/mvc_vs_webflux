package co.`fun`.joker

import co.`fun`.joker.api.ContentClassifyRequest
import co.`fun`.joker.api.ModerationPartialResponse
import co.`fun`.joker.api.ModerationRequest
import co.`fun`.joker.api.ModerationResponse
import co.`fun`.joker.util.DataCollector
import co.`fun`.joker.util.LoadTester
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
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

class LoadTest {
    private val loadData = File("times/test.txt").apply { parentFile.mkdirs() }
    private val collectData = File("times/test.csv").apply { parentFile.mkdirs() }

    private val ktorClientJava = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }

    private val host = "http://localhost:8080"
    private val simpleUrl = "$host/joker2021/simple"
    private val cpuLoadUrl = "$host/joker2021/cpu-load"
    private val chainUrl = "$host/joker2021/chain"
    private val moderationUrl = "$host/joker2021/moderation"

    private val classificationUrl = "${co.`fun`.joker.api.Env.SERVICE}/content/classify"

    @Test
    fun `test response`() = runBlockingWithDefersU {
        val response = request(cpuLoadUrl)
        println(response)
    }

    @Test
    fun `test moderation response`() = runBlockingWithDefersU {
        repeat(100) {
            val response = moderationRequest(moderationUrl, mockCalls = true)
            println(response)
        }
    }

    @Test
    fun `run io test`() = runBlockingWithDefersU {
        val steps = listOf(
            Duration.ofSeconds(0) to 10,
            Duration.ofSeconds(10) to 100,
            Duration.ofSeconds(50) to 300,
            Duration.ofSeconds(61) to 300,
        ).prepare()

        val tester = LoadTester(loadData, steps)
        tester.warming { moderationRequest(moderationUrl, mockCalls = true) }
        tester.loadTest { moderationRequest(moderationUrl, mockCalls = true) }

        DataCollector(loadData).collectData(collectData)
    }

    @Test
    fun `run cpu test`() = runBlockingWithDefersU {
        val steps = listOf(
            Duration.ofSeconds(0) to 1,
            Duration.ofSeconds(50) to 10,
            Duration.ofSeconds(60) to 10,
            Duration.ofSeconds(61) to 10,
        ).prepare()

        val tester = LoadTester(loadData, steps)
        tester.warming { request(cpuLoadUrl) }
        tester.loadTest { request(cpuLoadUrl) }

        DataCollector(loadData).collectData(collectData)
    }

    private fun List<Pair<Duration, Int>>.prepare(): List<Pair<Long, Int>> {
        return map { (duration, users) -> duration.toMillis() to users }
    }

    private suspend fun request(url: String): String = ktorClientJava.get(url)

    private suspend fun moderationRequest(
        moderationUrl: String,
        additionalDelay: Long? = null,
        mockCalls: Boolean = false
    ): ModerationResponse {
        return ktorClientJava.post(moderationUrl) {
            contentType(ContentType.Application.Json)
            body = ModerationRequest(
                UUID.randomUUID().toString(),
                Random.nextLong().toString(),
                additionalDelay = additionalDelay,
                mockCalls = mockCalls
            )
        }
    }

    private suspend fun classificationRequest(moderationUrl: String, additionalDelay: Long? = null): ModerationPartialResponse {
        return ktorClientJava.post(moderationUrl) {
            contentType(ContentType.Application.Json)
            body = ContentClassifyRequest(
                UUID.randomUUID().toString(),
                Random.nextLong().toString(),
                type = co.`fun`.joker.api.ContentType.IMAGE,
                additionalDelay = additionalDelay,
            )
        }
    }
}