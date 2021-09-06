package co.`fun`.joker.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/joker2021")
class SimpleController {
    @GetMapping("simple")
    fun get(): Mono<String> {
        return Mono.just("Just simple response =)")
    }
}