package co.`fun`.joker

import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.coroutineScope
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
    private val fileChannel = Channel<String>(capacity = UNLIMITED)
    private val fileWriteExecutor = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    private val ktorClientJava = HttpClient(Java)
    private val host = "http://localhost:8080/joker2021/simple"

    private val warmingTime = ofSeconds(30).toMillis()
    private val warmingTasks = 100

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
        val response = request()
        println(response)
    }

    @Test
    fun `run load test`() = runBlockingWithDefersU {
        val output = loadData.bufferedWriter().apply { defer { close() } }

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        warming()
        val fileJob = runFileWriteJob(output)
        val currentTasks = loadTest(Instant.now().toEpochMilli(), scope)

        currentTasks.forEach { it.cancel() }
        fileChannel.close()
        fileJob.join()
    }

    private suspend fun runFileWriteJob(output: BufferedWriter): Job {
        return fileWriteExecutor.launch {
            for (line in fileChannel) {
                output.write(line)
            }
        }
    }

    private suspend fun warming() = coroutineScope {
        println("start warming..")
        val tasks = (1..warmingTasks).map {
            launch {
                while (coroutineContext.isActive) {
                    request()
                }
            }
        }
        delay(warmingTime)
        tasks.forEach { it.cancel() }
    }

    private suspend fun loadTest(startTime: Long, scope: CoroutineScope): MutableList<Job> {
        println("start load test..")

        var currentTime = Instant.now()
        val currentTasks = mutableListOf<Job>()
        var diff = currentTime.toEpochMilli() - startTime
        while (diff < loadTime) {
            scope.addMoreJobs(currentTasks, diff / 1000, startTime)

            delay(increaseTime)
            currentTime = Instant.now()
            diff = currentTime.toEpochMilli() - startTime
        }
        return currentTasks
    }

    private fun CoroutineScope.addMoreJobs(
        currentTasks: MutableList<Job>,
        seconds: Long,
        startTime: Long
    ) {
        val stepIndex = (steps.indexOfFirst { it.first / 1000 >= seconds } - 1).coerceAtLeast(0)

        val currentStep = steps[stepIndex]
        val nextStep = steps[(stepIndex + 1).coerceAtMost(steps.size - 1)]
        val delta = (nextStep.second - currentStep.second).toFloat() /
            ((nextStep.first - currentStep.first).coerceAtLeast(1) / 1000)

        val neededCount =
            currentStep.second + (delta * (seconds - currentStep.first / 1000).coerceAtLeast(0)).roundToInt()

        val current = currentTasks.size

        val needMore = (neededCount - current).coerceAtLeast(0)
        println("seconds=$seconds, users=${currentTasks.size} add more $needMore workers")
        repeat(needMore) {
            currentTasks.add(launch {
                while (coroutineContext.isActive) {
                    request(startTime, currentTasks.size)
                }
            })
        }
    }

    private suspend fun request(startTime: Long, users: Int): String? {
        val requestTime = Instant.now()
        val response = request()

        val responseTime = Instant.now().toEpochMilli()
        val diff = responseTime - requestTime.toEpochMilli()
        val diffFromStart = responseTime - startTime

        return response.getOrNull().also {
            if (it != null)
                fileChannel.send("$diffFromStart;$diff;$users;1\n")
            else
                fileChannel.send("$diffFromStart;$diff;$users;0\n")
        }
    }

    private suspend fun request() = runCatching { ktorClientJava.get<String>(host) }
}