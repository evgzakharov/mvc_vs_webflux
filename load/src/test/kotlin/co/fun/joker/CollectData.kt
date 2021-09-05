package co.`fun`.joker

import org.junit.jupiter.api.Test
import kotlin.math.floor
import kotlin.math.roundToInt

private class Stats(
    var requests: Int = 0,
    var users: MutableList<Int> = mutableListOf(),
    val times: MutableList<Int> = mutableListOf(),
    var failed: Int = 0,
)

class CollectData {
    @Test
    fun collectData() {
        val data = sortedMapOf<Int, Stats>()

        loadData.forEachLine { line ->
            val columns = line.split(";")

            val seconds = floor(columns[0].toFloat() / 1000).toInt()
            val diff = columns[1].toInt()
            val users = columns[2].toInt()
            val failed = columns[3] == "fail"

            data.getOrPut(seconds) { Stats() }.also {
                it.requests++
                it.times += diff
                it.users += users

                if (failed)
                    it.failed += 1
            }
        }

        data.forEach {
            val times = it.value.times.sorted()
            val users = avg(it.value.users).roundToInt()

            val latencyAvg = avg(times)
            val latencyP99 = avg(times.take(floor(times.size.toFloat() * 0.99).toInt()))
            val latencyP95 = avg(times.take(floor(times.size.toFloat() * 0.95).toInt()))

            println("${it.key};${it.value.requests};$users;${it.value.failed};$latencyAvg;$latencyP99;$latencyP95")
        }
    }

    private fun avg(times: List<Int>) = times.sum().toFloat() / times.size
}