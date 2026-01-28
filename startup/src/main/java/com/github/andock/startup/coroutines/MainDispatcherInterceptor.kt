package com.github.andock.startup.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal class MainDispatcherInterceptor(
    mainDispatcher: CoroutineDispatcher,
) : ContextElementInterceptor<CoroutineDispatcher> {

    override val key = CoroutineDispatcher.intercept()
    private val main = BlockingMainCoroutineDispatcher(mainDispatcher.asDelayable())

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