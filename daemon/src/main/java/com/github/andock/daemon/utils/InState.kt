package com.github.andock.daemon.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass

typealias InState<T> = Pair<StateFlow<T>, Set<KClass<out T>>>

@PublishedApi
internal suspend inline fun InState<*>.executeUnchecked(
    crossinline block: suspend () -> Any?
): Any? {
    val (state, classes) = this
    val collector = object : CancellationException(
        "Flow was aborted, no more elements needed"
    ), suspend (Boolean) -> Unit {
        var result: Any? = this

        override fun fillInStackTrace(): Throwable {
            stackTrace = emptyArray()
            return this
        }

        override suspend fun invoke(value: Boolean) {
            if (value) {
                result = block()
                throw this
            } else {
                throw IllegalStateException("container is not $classes")
            }
        }
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
    if (collector.result == collector) {
        throw NoSuchElementException("Expected at least one element")
    }
    return collector.result
}

suspend inline fun <reified R> InState<*>.execute(
    crossinline block: suspend () -> R
): R {
    return executeUnchecked { block() } as R
}

fun <T> StateFlow<T>.inState(vararg classes: KClass<out T>): InState<T> {
    return this to setOf(*classes)
}
