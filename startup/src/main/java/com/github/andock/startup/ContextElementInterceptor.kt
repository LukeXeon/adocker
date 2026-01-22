package com.github.andock.startup

import kotlin.coroutines.CoroutineContext

internal interface ContextElementInterceptor<E : CoroutineContext.Element> :
    CoroutineContext.Element {

    val target: CoroutineContext.Key<E>

    fun intercept(interceptor: E): E

    fun intercept(context: CoroutineContext): E? {
        return intercept(context[target] ?: return null)
    }

    companion object {
        operator fun <E : CoroutineContext.Element> invoke(): CoroutineContext.Key<ContextElementInterceptor<E>> {
            return object : CoroutineContext.Key<ContextElementInterceptor<E>> {}
        }
    }
}