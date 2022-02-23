package co.`fun`.compare

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

@DslMarker
annotation class DeferDsl

class CoroutineScopeWithDeferrer(scope: CoroutineScope, defer: Deferrer): CoroutineScope by scope, Deferrer by defer

interface Deferrer {
    @DeferDsl
    fun defer(action: () -> Unit)
}

class DeferrerImpl: Deferrer {
    private val actions = arrayListOf<() -> Unit>()

    override fun defer(action: () -> Unit) {
        actions.add(action)
    }

    fun done() {
        actions.reversed().forEach { it() }
    }
}

inline fun <T> withDefers(body: Deferrer.() -> T): T {
    val deferrer = DeferrerImpl()
    try {
        return deferrer.body()
    } finally {
        deferrer.done()
    }
}

fun <T> runBlockingWithDefers(body: suspend CoroutineScopeWithDeferrer.() -> T): T {
    return runBlocking {
        withDefers {
            CoroutineScopeWithDeferrer(this@runBlocking, this).body()
        }
    }
}

fun <T> runBlockingWithDefersU(body: suspend CoroutineScopeWithDeferrer.() -> T): Unit =
    runBlockingWithDefers { body() }
