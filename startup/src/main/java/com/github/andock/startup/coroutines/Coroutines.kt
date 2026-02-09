@file:OptIn(InternalCoroutinesApi::class)

package com.github.andock.startup.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Delay
import kotlinx.coroutines.InternalCoroutinesApi
import kotlin.coroutines.CoroutineContext


internal fun <T> CoroutineDispatcher.asDelayable(): T where T : CoroutineDispatcher, T : Delay {
    require(CoroutineDispatcher::class.isInstance(this) && Delay::class.isInstance(this))
    @Suppress("UNCHECKED_CAST")
    return this as T
}

internal fun <E : CoroutineContext.Element> ContextElementInterceptor<E>.intercept(context: CoroutineContext): E? {
    return intercept(context[key.target] ?: return null)
}

internal fun <E : CoroutineContext.Element> CoroutineContext.Key<E>.interceptor(): ContextElementInterceptor.Key<E> {
    return ContextElementInterceptor.Key(this)
}