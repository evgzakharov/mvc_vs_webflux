package co.`fun`.joker

import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

class MonoCheck {
    @Test
    fun `check mono`() {
        val test = Mono.create<Int> { println("create"); it.success(1) }
            .cache()

        test.map { it * 2 }.block()
        test.map { it * 2 }.block()
    }
}