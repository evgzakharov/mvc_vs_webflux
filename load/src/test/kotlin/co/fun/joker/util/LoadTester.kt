package co.`fun`.joker.util

import co.`fun`.joker.runBlockingWithDefersU
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
import java.io.BufferedWriter
import java.io.File
import java.time.Duration.ofSeconds
import java.time.Instant
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class LoadTester(
    private val loadData: File,
    private val steps: List<Pair<Long, Int>>
) {
    private val fileChannel = Channel<String>(capacity = UNLIMITED)
    private val fileWriteExecutor = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    private val warmingTime = ofSeconds(30).toMillis()
    private val warmingTasks = 100
    private val loadTime = steps.last().first
    private val increaseTime = ofSeconds(1).toMillis()

    suspend fun <T> loadTest(request: suspend () -> T) = runBlockingWithDefersU {
        val output = loadData.bufferedWriter().apply { defer { close() } }

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        warming(request)
        val fileJob = runFileWriteJob(output)
        val currentTasks = loadTest(Instant.now().toEpochMilli(), scope, request)

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

    private suspend fun <T> warming(request: suspend () -> T) = coroutineScope {
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

    private suspend fun <T> loadTest(
        startTime: Long,
        scope: CoroutineScope,
        request: suspend () -> T
    ): MutableList<Job> {
        println("start load test..")

        var currentTime = Instant.now()
        val currentTasks = mutableListOf<Job>()
        var diff = currentTime.toEpochMilli() - startTime
        while (diff < loadTime) {
            scope.addMoreJobs(currentTasks, diff / 1000, startTime, request)

            delay(increaseTime)
            currentTime = Instant.now()
            diff = currentTime.toEpochMilli() - startTime
        }
        return currentTasks
    }

    private fun <T> CoroutineScope.addMoreJobs(
        currentTasks: MutableList<Job>,
        seconds: Long,
        startTime: Long,
        taskAction: suspend () -> T
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
                    request(startTime, currentTasks.size, taskAction)
                }
            })
        }
    }

    private suspend fun <T> request(startTime: Long, users: Int, taskAction: suspend () -> T): T? {
        val requestTime = Instant.now()
        val response = runCatching { taskAction() }

        val responseTime = Instant.now().toEpochMilli()
        val diff = responseTime - requestTime.toEpochMilli()
        val diffFromStart = responseTime - startTime

        return response.getOrNull().also {
            if (response.isSuccess)
                fileChannel.send("$diffFromStart;$diff;$users;1\n")
            else
                fileChannel.send("$diffFromStart;$diff;$users;0\n")
        }
    }
}