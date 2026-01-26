package com.github.andock.startup.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

internal class MainDispatcherInterceptor(
    mainDispatcher: CoroutineDispatcher,
) : AbstractCoroutineContextElement(contextElementInterceptor<CoroutineDispatcher>()),
    ContextElementInterceptor<CoroutineDispatcher> {

    private val main = BlockingMainCoroutineDispatcher(mainDispatcher.asDelayable())

    override val target: CoroutineContext.Key<CoroutineDispatcher>
        get() = CoroutineDispatcher

    override fun intercept(interceptor: CoroutineDispatcher): CoroutineDispatcher {
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