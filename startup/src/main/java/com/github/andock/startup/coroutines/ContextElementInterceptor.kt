package com.github.andock.startup.coroutines

import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

internal interface ContextElementInterceptor<E : CoroutineContext.Element> :
    CoroutineContext.Element {

    val target: CoroutineContext.Key<E>

    fun intercept(interceptor: E): E

    data class Key<E : CoroutineContext.Element>(
        val type: KClass<E>
    ) : CoroutineContext.Key<ContextElementInterceptor<E>>
}

