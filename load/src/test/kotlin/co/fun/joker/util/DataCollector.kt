package co.`fun`.joker.util

import co.`fun`.joker.runBlockingWithDefersU
import java.io.File
import kotlin.math.floor
import kotlin.math.roundToInt

private class Stats(
    var requests: Int = 0,
    var users: MutableList<Int> = mutableListOf(),
    val times: MutableList<Int> = mutableListOf(),
    var failed: Int = 0,
)

class DataCollector(private val loadData: File) {
    fun collectData(resultFile: File) = runBlockingWithDefersU {
        val output = resultFile.bufferedWriter().apply { defer { close() } }

        val data = sortedMapOf<Int, Stats>()

        loadData.forEachLine { line ->
            val columns = line.split(";")

            val seconds = floor(columns[0].toFloat() / 1000).toInt()
            val diff = columns[1].toInt()
            val users = columns[2].toInt()
            val failed = columns[3] == "0"

            data.getOrPut(seconds) { Stats() }.also {
                it.requests++
                it.times += diff
                it.users += users

                if (failed)
                    it.failed += 1
            }
        }

        output.write("TIME,RPS,USERS,ERRORS,LAVG,L99,L95")
        output.newLine()

        data.forEach {
            val times = it.value.times.sorted()
            val users = avg(it.value.users).roundToInt()

            val latencyAvg = avg(times)
            val latencyP99 = avg(times.take(floor(times.size.toFloat() * 0.99).toInt()))
            val latencyP95 = avg(times.take(floor(times.size.toFloat() * 0.95).toInt()))

            output.write("${it.key},${it.value.requests},$users,${it.value.failed},$latencyAvg,$latencyP99,$latencyP95")
            output.newLine()
        }
    }

    private fun avg(times: List<Int>) = times.sum().toFloat() / times.size
}