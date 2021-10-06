package co.`fun`.joker.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/joker2021")
class SimpleController {
    @GetMapping("simple")
    fun get(): Mono<String> {
        return Mono.just("Just simple response =)")
    }

    @GetMapping("cpu-load")
    fun cpuLoad(): Mono<Int> {
        var sum = 0
        repeat(5_000_000) { i ->
            sum += i
        }
        return Mono.just(sum)
    }

    @GetMapping("chain-mono")
    fun chainMono(): Mono<Int> {
        var result = Mono.just(5)

        for (i in 1..1_000) {
            result = result.map { it + i }
        }

        return result
    }

    @GetMapping("chain-flux")
    fun chainFlux(): Mono<Int> {
        var result = Flux.just(5)

        for (i in 1..1_000) {
            result = result.map { it + i }
        }

        return result.single()
    }
}