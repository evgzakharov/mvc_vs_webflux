package co.`fun`.compare.problems

import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

class Methods {
    @Test
    fun test() {
        val mono = getMono()

        println(mono.block())
    }

    private fun getMono(): Mono<String> {
        return Mono.just("test")
    }
}