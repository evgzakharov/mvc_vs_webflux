package co.`fun`.joker

import co.`fun`.joker.util.DataCollector
import co.`fun`.joker.util.LoadTester
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.request.get
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Duration

val loadData = File("times/test.txt").apply { parentFile.mkdirs() }
val collectData = File("times/test.csv").apply { parentFile.mkdirs() }

class LoadTest {
    private val ktorClientJava = HttpClient(Java)

    private val simpleUrl = "http://localhost:8080/joker2021/simple"
    private val cpuLoadUrl = "http://localhost:8080/joker2021/cpu-load"
    private val chainUrl = "http://localhost:8080/joker2021/chain"

    @Test
    fun `test response`() = runBlockingWithDefersU {
        val response = request(chainUrl)
        println(response)
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
        tester.loadTest { request(chainUrl) }

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

    private suspend fun request(url: String) = runCatching { ktorClientJava.get<String>(url) }
}