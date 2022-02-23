package co.`fun`.compare.problems

import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

class ZipCheck {
    @Test
    fun zip() {
        val result = Mono.zip(
            Mono.just("123"),
            Mono.just("321"),
            Mono.just("555"),
            Mono.empty<String>()
        )

        println(result.block())
    }
}