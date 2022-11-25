package co.`fun`.compare

import co.`fun`.compare.api.Env
import co.`fun`.compare.api.ModerationRequest
import co.`fun`.compare.util.DataCollector
import co.`fun`.compare.util.HttpRequester
import co.`fun`.compare.util.LoadTester
import co.`fun`.compare.util.Method
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readText
import io.ktor.serialization.kotlinx.json.json
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Duration
import kotlin.random.Random

class LoadTest {
    private val loadData = File("times/test.txt").apply { parentFile.mkdirs() }
    private val collectData = File("times/test.csv").apply { parentFile.mkdirs() }

    private val ktorClientJava = HttpClient(Apache) {
        install(ContentNegotiation) {
            json()
        }
    }

    private val requester = (1..10).map { HttpRequester() }

//    private val host = "http://0.0.0.0:8080"
    private val host = Env.TEST_SERVICE

    private val simpleUrl = "$host/compare/simple"
    private val cpuLoadUrl = "$host/compare/cpu-load"

    private val chainMono =  "$host/compare/chain-mono"
    private val chainFlux =  "$host/compare/chain-flux"
    private val chainFlow =  "$host/compare/chain-flow"
    private val chainSuspend =  "$host/compare/chain-suspend"

    private val moderationUrl = "$host/compare/moderation"

    @Test
    fun `test response`() = runBlockingWithDefersU {
        val response = moderationRequest(moderationUrl, 100L, mockCalls = true)
        println(response)
    }

    @Test
    fun `test moderation response`() = runBlockingWithDefersU {
        repeat(100) {
            val response = moderationRequest(moderationUrl, delay = null, mockCalls = true)
            println(response)
        }
    }

    @Test
    fun `run io test`() = runBlockingWithDefersU {
        val steps = listOf(
            Duration.ofSeconds(0) to 1,
            Duration.ofSeconds(10) to 100,
            Duration.ofSeconds(50) to 100,
            Duration.ofSeconds(61) to 300,
        ).prepare()

        val tester = LoadTester(loadData, steps)
        tester.warming(400) { request(simpleUrl) }
        tester.loadTest { request(simpleUrl) }

        DataCollector(loadData).collectData(collectData)
    }

    @Test
    fun `run moderation test`() = runBlockingWithDefersU {
        val steps = listOf(
            Duration.ofSeconds(0) to 1,
            Duration.ofSeconds(10) to 10,
            Duration.ofSeconds(50) to 300,
            Duration.ofSeconds(61) to 300,
        ).prepare()

        val tester = LoadTester(loadData, steps)
        tester.warming(100) { moderationRequest(moderationUrl, delay = null, mockCalls = false) }
        tester.loadTest { moderationRequest(moderationUrl, delay = 100, mockCalls = false) }

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
        tester.warming(100) { request(cpuLoadUrl) }
        tester.loadTest { request(cpuLoadUrl) }

        DataCollector(loadData).collectData(collectData)
    }

    private fun List<Pair<Duration, Int>>.prepare(): List<Pair<Long, Int>> {
        return map { (duration, users) -> duration.toMillis() to users }
    }

    private suspend fun request(url: String): String? = requester.random().sendRequest(url)
//    private suspend fun request(url: String): String = ktorClientJava.get(url).bodyAsText()

    private suspend fun moderationRequest(url: String, delay: Long? = null, mockCalls: Boolean = false): String? = requester.random().sendRequest(
        url,
        method = Method.POST,
        data = ModerationRequest(
            Random.nextLong().toString(),
            Random.nextLong().toString(),
            additionalDelay = delay,
            mockCalls = mockCalls
        )
    )
}