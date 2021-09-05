package co.`fun`.joker

import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Test
import java.io.BufferedWriter
import java.io.File
import java.time.Duration.ofSeconds
import java.time.Instant
import java.util.concurrent.Executors
import kotlin.math.roundToInt

val loadData = File("times/test.txt").apply { parentFile.mkdirs() }

class LoadTest {
    private val fileJob = Job()
    private val fileScope = CoroutineScope(fileJob + Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    private val ktorClientJava = HttpClient(Java)
    private val host = "http://localhost:8080/joker2021/simple"

    private val warmingTime = ofSeconds(10)
    private val warmingTasks = 1000

    private val steps = listOf(
        ofSeconds(0).toMillis() to 10,
        ofSeconds(10).toMillis() to 100,
        ofSeconds(20).toMillis() to 150,
        ofSeconds(30).toMillis() to 200,
        ofSeconds(40).toMillis() to 250,
        ofSeconds(50).toMillis() to 300,
        ofSeconds(61).toMillis() to 300,
    )
    private val loadTime = steps.last().first
    private val increaseTime = ofSeconds(1).toMillis()

    @Test
    fun `test response`() = runBlockingWithDefersU {
        val response = request(Instant.now().toEpochMilli(), 0, null)
        println(response)
    }

    @Test
    fun `run load test`() = runBlockingWithDefersU {
        val output = loadData.bufferedWriter().apply { defer { close() } }

        val startTime = Instant.now().toEpochMilli()

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        var currentTime = Instant.now()
        var currentTasks = mutableListOf<Job>()
        var diff = currentTime.toEpochMilli() - startTime

        while (diff < loadTime) {
            scope.addMoreJobs(currentTasks, diff / 1000, output, startTime)

            delay(increaseTime)
            currentTime = Instant.now()
            diff = currentTime.toEpochMilli() - startTime
        }

        currentTasks.forEach { it.cancel() }
        fileJob.children.forEach { it.join() }
    }

    private fun CoroutineScope.addMoreJobs(
        currentTasks: MutableList<Job>,
        seconds: Long,
        output: BufferedWriter,
        startTime: Long
    ) {
        val stepIndex = (steps.indexOfFirst { it.first / 1000 >= seconds } - 1).coerceAtLeast(0)

        val currentStep = steps[stepIndex]
        val nextStep = steps[(stepIndex + 1).coerceAtMost(steps.size - 1)]
        val delta = (nextStep.second - currentStep.second).toFloat() /
            ((nextStep.first - currentStep.first).coerceAtLeast(1) / 1000)

        val neededCount = currentStep.second + (delta * (seconds - currentStep.first / 1000).coerceAtLeast(0)).roundToInt()

        val current = currentTasks.size

        val needMore = (neededCount - current).coerceAtLeast(0)
        println("seconds=$seconds, users=${currentTasks.size} add more $needMore workers")
        repeat(needMore) {
            currentTasks.add(launch {
                while (coroutineContext.isActive) {
                    request(startTime, currentTasks.size, output)
                }
            })
        }
    }

    private suspend fun request(startTime: Long, users: Int, output: BufferedWriter? = null): String? {
        val requestTime = Instant.now()
        val response = runCatching { ktorClientJava.get<String>(host) }

        val responseTime = Instant.now().toEpochMilli()
        val diff = responseTime - requestTime.toEpochMilli()
        val diffFromStart = responseTime - startTime

        return response.getOrNull().also {
            fileScope.launch {
                if (it != null)
                    output?.write("$diffFromStart;$diff;$users;success\n")
                else
                    output?.write("$diffFromStart;$diff;$users;fail\n")
            }
        }
    }
}