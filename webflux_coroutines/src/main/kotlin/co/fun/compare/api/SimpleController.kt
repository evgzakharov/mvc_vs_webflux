package co.`fun`.compare.api

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/compare")
class SimpleController {
    @GetMapping("simple")
    fun get(): String {
        return "Just simple response =)"
    }

    @GetMapping("cpu-load")
    suspend fun cpuLoad(): Int {
        var sum = 0
        repeat(5_000_000) { i ->
            sum += i
        }
        return sum
    }

    @GetMapping("chain-flow")
    suspend fun chainFlow(): Int {
        var result = flowOf(5)

        for (i in 1..1_000) {
            result = result.map { it + i }
        }

        return result.first()
    }

    @GetMapping("chain-suspend")
    suspend fun chainSuspend(): Int {
        var result = 5

        for (i in 1..1_000) {
            result = result.chainChange { it + i }
        }

        return result
    }

    private fun Int.chainChange(block: (Int) -> Int): Int {
        return block(this)
    }
}