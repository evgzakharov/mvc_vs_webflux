package co.`fun`.joker.api

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/joker2021")
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

    @GetMapping("chain")
    suspend fun chain(): Int {
        var result = flowOf(5)

        for (i in 1..1_000) {
            result = result.map { it + i }
        }

        return result.first()
    }
}