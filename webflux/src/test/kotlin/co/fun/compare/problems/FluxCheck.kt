package co.`fun`.compare.problems

import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.time.Instant

class FluxCheck {
    @Test
    fun fluxAll() {
        Flux.just("data")
            .doOnNext {
                println(
                    "just ${Thread.currentThread().name}"
                )
            }
//            .publishOn(Schedulers.parallel())
            .doOnNext {
                println(
                    "publish ${Thread.currentThread().name}"
                )
            }
            .delayElements(Duration.ofMillis(500))
            .subscribeOn(Schedulers.single())
            .doOnNext {
                println(
                    "after subscribe ${Thread.currentThread().name}"
                )
            }
            .subscribe {
                println(
                    "subscribe ${Thread.currentThread().name}"
                )
            }

        Thread.sleep(1000)
    }

    @Test
    fun flux() {
        val start = Instant.now().toEpochMilli()

        Flux.fromIterable((1..10).map { it })
            .flatMap({ count ->
                val subscribeOn: Mono<Int> = Mono.defer {
                    Thread.sleep(1000 * count.toLong())

                    val time = Instant.now()
                    println("${Thread.currentThread().name} ${time.toEpochMilli() - start}")
                    Mono.just(count)
                }.subscribeOn(Schedulers.boundedElastic())

                subscribeOn
            }, 1)
            .subscribe()

        Thread.sleep(30000)
    }

    @Test
    fun fluxParallel() {
        val start = Instant.now().toEpochMilli()

        Flux.fromIterable((1..10).map { it })
            .parallel()
            .runOn(Schedulers.boundedElastic())
            .map { count ->
                Thread.sleep(1000 * count.toLong())

                val time = Instant.now()
                println("${Thread.currentThread().name} ${time.toEpochMilli() - start}")
            }
            .subscribe()

        Thread.sleep(30000)
    }
}