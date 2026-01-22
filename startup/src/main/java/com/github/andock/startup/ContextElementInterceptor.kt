package com.github.andock.startup

import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

internal interface ContextElementInterceptor<E : CoroutineContext.Element> :
    CoroutineContext.Element {

    val target: CoroutineContext.Key<E>

    fun intercept(interceptor: E): E

    data class Key<E : CoroutineContext.Element>(val type: KClass<E>) :
        CoroutineContext.Key<ContextElementInterceptor<E>>

    companion object {
        inline operator fun <reified E : CoroutineContext.Element> invoke(): CoroutineContext.Key<ContextElementInterceptor<E>> {
            return Key(E::class)
        }

        internal fun <E : CoroutineContext.Element> ContextElementInterceptor<E>.intercept(context: CoroutineContext): E? {
            return intercept(context[target] ?: return null)
        }
    }
}

