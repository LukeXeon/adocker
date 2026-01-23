package com.github.andock.startup.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

internal class MainDispatcherInterceptor(
    mainDispatcher: CoroutineDispatcher,
) : AbstractCoroutineContextElement(ContextElementInterceptor<ContinuationInterceptor>()),
    ContextElementInterceptor<ContinuationInterceptor> {
    private val main = EventLoopMainDispatcher(mainDispatcher)

    override val target: CoroutineContext.Key<ContinuationInterceptor>
        get() = ContinuationInterceptor.Key

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