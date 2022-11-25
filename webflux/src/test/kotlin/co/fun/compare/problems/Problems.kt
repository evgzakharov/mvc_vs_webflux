package co.`fun`.compare.problems

import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

class Problems {
    @Test
    fun cache() {
        val mono: Mono<String> = Mono.fromCallable {
            println("create new string")
            "test"
        }

        val mono2 = mono.map { it + "2" }

        println(mono.block())
        println(mono2.block())
    }

    @Test
    fun cache2() {
        val mono: Mono<String> = producer()
        val mono2 = mono.map { it + "2" }

        println(mono.block())
        println(mono2.block())
    }

    private fun producer(): Mono<String> {
        println("create new string from producer")
        return Mono.just("123")
    }
}