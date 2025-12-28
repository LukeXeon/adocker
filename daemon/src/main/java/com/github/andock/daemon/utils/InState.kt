package com.github.andock.daemon.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass

typealias InState<T> = Pair<StateFlow<T>, Set<KClass<out T>>>

@PublishedApi
internal abstract class Collector(
    private val inState: InState<*>,
) : CancellationException(
    "Flow was aborted, no more elements needed"
), suspend (Boolean) -> Unit {

    var result: Any? = this

    override fun fillInStackTrace(): Throwable {
        stackTrace = emptyArray()
        return this
    }

    override suspend fun invoke(value: Boolean) {
        if (value) {
            result = execute()
            throw this
        } else {
            throw IllegalStateException("${inState.first} is not ${inState.second}")
        }
    }

    abstract suspend fun execute(): Any?
}

@PublishedApi
internal inline fun Collector(
    inState: InState<*>,
    crossinline block: suspend () -> Any?
): Collector {
    return object : Collector(inState) {
        override suspend fun execute() = block()
    }
}

suspend inline fun <reified R> InState<*>.execute(
    crossinline block: suspend () -> R
): R {
    val (state, classes) = this
    val collector = Collector(this) {
        block()
    }
    try {
        state.map { state ->
            classes.any { clazz ->
                clazz.isInstance(state)
            }
        }.distinctUntilChanged().collectLatest(collector)
    } catch (e: Collector) {
        if (e != collector) {
            throw e
        }
    }
    if (collector.result == collector) {
        throw NoSuchElementException("Expected at least one element")
    }
    return collector.result as R
}

fun <T : Any> StateFlow<T>.inState(vararg classes: KClass<out T>): InState<T> {
    return this to setOf(*classes)
}
