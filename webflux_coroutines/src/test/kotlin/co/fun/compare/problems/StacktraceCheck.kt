package co.`fun`.compare.problems

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class StacktraceCheckCoroutines {
    @Test
    fun stacktraceCoroutines() = runBlocking {
        var a = 10
        var b = 2

        someAction {
            a += 1
        }

        someAction {
//            println(a)
            b += 1
        }

        someAction {
            b += 1
        }

        println(a)
        println(b)
    }

    suspend fun someAction(action: suspend () -> Unit) {
        action()
    }
}