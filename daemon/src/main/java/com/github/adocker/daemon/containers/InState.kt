package com.github.adocker.daemon.containers

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass

typealias InState = Pair<StateFlow<ContainerState>, Set<KClass<*>>>

@PublishedApi
internal suspend inline fun InState.executeInternal(
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
        state.map {
            classes.any { clazz ->
                clazz.isInstance(it)
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

suspend inline fun <reified R> InState.execute(
    crossinline block: suspend () -> R
): R {
    return executeInternal { block() } as R
}

inline fun <reified S : ContainerState> StateFlow<ContainerState>.inState(): InState {
    return this to setOf(S::class)
}

fun StateFlow<ContainerState>.inState(vararg classes: KClass<out ContainerState>): InState {
    return this to setOf(*classes)
}
