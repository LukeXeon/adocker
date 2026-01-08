package com.github.andock.daemon.utils

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

typealias InState<T> = Pair<StateFlow<T>, Set<KClass<out T>>>

suspend inline fun <reified R> InState<*>.execute(
    crossinline block: suspend () -> R
): R {
    val (state, classes) = this
    return coroutineScope {
        val job = async {
            block()
        }
        launch {
            state.map { state ->
                classes.any { clazz ->
                    clazz.isInstance(state)
                }
            }.distinctUntilChanged().collect {
                if (!it) {
                    job.cancel()
                }
            }
        }
        job.await()
    }
}

fun <T : Any> StateFlow<T>.inState(vararg classes: KClass<out T>): InState<T> {
    return this to setOf(*classes)
}
