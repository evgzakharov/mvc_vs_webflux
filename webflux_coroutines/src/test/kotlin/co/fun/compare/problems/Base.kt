@file:Suppress("DeferredResultUnused")

package co.`fun`.compare.problems

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.debug.DebugProbes
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class Base {
    @Test
    fun test() = runBlocking {
//        DebugProbes.install()

        try {
            method()
        } catch (e: Exception) {
            println("catched outer")
//            DebugProbes.dumpCoroutines()
        }
        Unit
    }

    private suspend fun method(): Unit = coroutineScope {
        try {
            async {
                throw RuntimeException("blaah")
            }.await()
        } catch (e: Exception) {
            println("catched")
            async {
                println("catched2")
            }
        } finally {
            println("finally")
        }

    }
}

