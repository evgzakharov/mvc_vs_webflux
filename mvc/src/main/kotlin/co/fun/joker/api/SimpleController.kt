package co.`fun`.joker.api

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
    fun cpuLoad(): Int {
        var sum = 0
        repeat(5_000_000) { i ->
            sum += i
        }
        return sum
    }
}