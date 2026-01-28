package com.github.andock.startup.coroutines

import kotlin.coroutines.CoroutineContext

internal interface ContextElementInterceptor<E : CoroutineContext.Element> :
    CoroutineContext.Element {

    val target: CoroutineContext.Key<E>

    fun intercept(interceptor: E): E

    data class Key<E : CoroutineContext.Element>(
        val intercepted: CoroutineContext.Key<E>
    ) : CoroutineContext.Key<ContextElementInterceptor<E>>
}

