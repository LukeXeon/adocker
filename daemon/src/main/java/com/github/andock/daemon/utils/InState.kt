package com.github.andock.daemon.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.util.concurrent.Callable
import kotlin.reflect.KClass

typealias InState<T> = Pair<StateFlow<T>, Set<KClass<out T>>>

@PublishedApi
internal inline fun InState<*>.collector(
    crossinline block: suspend () -> Any?
): suspend (Boolean) -> Unit {
    return object : CancellationException(
        "Flow was aborted, no more elements needed"
    ), Callable<Any?>, suspend (Boolean) -> Unit {
        private var value: Any? = this

        override fun fillInStackTrace(): Throwable {
            stackTrace = emptyArray()
            return this
        }

        override suspend fun invoke(value: Boolean) {
            if (value) {
                this.value = block()
                throw this
            } else {
                throw IllegalStateException("$first is not $second")
            }
        }

        override fun call(): Any? {
            if (value == this) {
                throw NoSuchElementException("Expected at least one element")
            }
            return value
        }
    }
}

suspend inline fun <reified R> InState<*>.execute(
    crossinline block: suspend () -> R
): R {
    val (state, classes) = this
    val collector = collector {
        block()
    }
    try {
        state.map { state ->
            classes.any { clazz ->
                clazz.isInstance(state)
            }
        }.distinctUntilChanged().collectLatest(collector)
    } catch (e: CancellationException) {
        if (e != collector) {
            throw e
        }
    }
    return (collector as Callable<*>).call() as R
}

fun <T : Any> StateFlow<T>.inState(vararg classes: KClass<out T>): InState<T> {
    return this to setOf(*classes)
}
