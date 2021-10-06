package co.`fun`.joker.problems

import org.junit.jupiter.api.Test
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono

class StacktraceCheck {
    @Test
    fun stacktrace() {
        Hooks.onOperatorDebug()

        val result = Mono.just(1)
            .map { it + 1 }
            .map { it + 1 }
            .map {
                it + 1
            }
            .flatMap { Mono.defer<Int> {
                throw RuntimeException(it.toString())
            } }

        println(result.block())
    }
}