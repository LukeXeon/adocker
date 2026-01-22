package com.github.andock.startup

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.ContinuationInterceptor

internal class RedirectDispatchers(
    mainDispatcher: CoroutineDispatcher,
) : AbstractCoroutineContextElement(ContinuationInterceptorInterceptor),
    ContinuationInterceptorInterceptor {
    private val main = EventLoopMainCoroutineDispatcher(mainDispatcher)

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