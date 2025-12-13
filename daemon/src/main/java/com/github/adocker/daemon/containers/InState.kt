package com.github.adocker.daemon.containers

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass

@PublishedApi
internal class AbortFlowException(private val owner: Any) : CancellationException(
    "Flow was aborted, no more elements needed"
) {
    override fun fillInStackTrace(): Throwable {
        stackTrace = emptyArray()
        return this
    }

    fun checkOwnership(owner: Any) {
        if (this.owner !== owner) throw this
    }
}

typealias InState = Pair<StateFlow<ContainerState>, Set<KClass<*>>>

suspend inline fun <reified R> InState.execute(
    crossinline block: suspend () -> R
): R {
    val (state, classes) = this
    val collector = object : suspend (Boolean) -> Unit {
        var result: Any? = this

        override suspend fun invoke(value: Boolean) {
            if (value) {
                result = block()
                throw AbortFlowException(this)
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
    } catch (e: AbortFlowException) {
        e.checkOwnership(collector)
    }
    if (collector.result == collector) {
        throw NoSuchElementException("Expected at least one element")
    }
    return collector.result as R
}

inline fun <reified S : ContainerState> StateFlow<ContainerState>.inState(): InState {
    return this to setOf(S::class)
}

fun StateFlow<ContainerState>.inState(vararg classes: KClass<out ContainerState>): InState {
    return this to setOf(*classes)
}
