@file:OptIn(InternalCoroutinesApi::class)

package com.github.andock.startup.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Delay
import kotlinx.coroutines.InternalCoroutinesApi
import kotlin.coroutines.CoroutineContext


private val mainDispatcherTypes = arrayOf(CoroutineDispatcher::class, Delay::class)

internal fun <T> CoroutineDispatcher.asDelayable(): T where T : CoroutineDispatcher, T : Delay {
    require(mainDispatcherTypes.all { it.isInstance(this) })
    @Suppress("UNCHECKED_CAST")
    return this as T
}

internal fun <E : CoroutineContext.Element> ContextElementInterceptor<E>.intercept(context: CoroutineContext): E? {
    return intercept(context[target] ?: return null)
}

internal inline fun <reified E : CoroutineContext.Element> contextElementInterceptorKey(): CoroutineContext.Key<ContextElementInterceptor<E>> {
    return ContextElementInterceptor.Key(E::class)
}