package com.github.andock.daemon.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass

typealias InState<T> = Pair<StateFlow<T>, Set<KClass<out T>>>

@PublishedApi
internal suspend inline fun executeImpl(
    state: StateFlow<*>,
    classes: Set<KClass<*>>,
    crossinline block: suspend () -> Any?
): Any? {
    val collector = object : CancellationException(
        "Flow was aborted, no more elements needed"
    ), suspend (Boolean) -> Unit {
        var result: Any? = this

        override fun fillInStackTrace(): Throwable {
            stackTrace = emptyArray()
            return this
        }

        override suspend fun invoke(value: Boolean) {
            check(value) { "$state is not $classes" }
            result = block()
            throw this
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
    check(collector.result != collector)
    return collector.result
}

suspend inline fun <reified R> InState<*>.execute(
    crossinline block: suspend () -> R
): R {
    return executeImpl(first, second, block) as R
}

fun <T : Any> StateFlow<T>.inState(vararg classes: KClass<out T>): InState<T> {
    return this to setOf(*classes)
}
