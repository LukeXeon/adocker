package com.github.andock.startup

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.ContinuationInterceptor

internal class RedirectDispatchers(
    thread: Thread,
    dispatcher: CoroutineDispatcher,
) : AbstractCoroutineContextElement(ContinuationInterceptorInterceptor),
    ContinuationInterceptorInterceptor {
    private val main = EventLoopMainCoroutineDispatcher(thread, dispatcher)

    override fun intercept(interceptor: ContinuationInterceptor): ContinuationInterceptor {
        return when (interceptor) {
            Dispatchers.Main -> {
                main
            }

            Dispatchers.Main.immediate -> {
                main.immediate
            }

            else -> {
                interceptor
            }
        }
    }
}