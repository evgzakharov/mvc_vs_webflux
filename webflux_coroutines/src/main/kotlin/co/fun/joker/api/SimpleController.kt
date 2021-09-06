package co.`fun`.joker.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/joker2021")
class SimpleController {
    @GetMapping("simple")
    suspend fun get(): String {
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
        var result = 5

        for (i in 1..10_000) {
            result = result.transform { it + i }
        }

        return result
    }

    private suspend fun Int.transform(action: (Int) -> Int): Int {
        return action(this)
    }
}