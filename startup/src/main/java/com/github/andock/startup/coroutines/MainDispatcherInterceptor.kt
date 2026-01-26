package com.github.andock.startup.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

internal class MainDispatcherInterceptor(
    mainDispatcher: CoroutineDispatcher,
) : AbstractCoroutineContextElement(ContextElementInterceptor<CoroutineDispatcher>()),
    ContextElementInterceptor<CoroutineDispatcher> {

    @OptIn(InternalCoroutinesApi::class)
    companion object {
        private val types = arrayOf(CoroutineDispatcher::class, Delay::class)

        private fun <T> CoroutineDispatcher.asDelayable(): T where T : CoroutineDispatcher, T : Delay {
            require(types.all { it.isInstance(this) })
            @Suppress("UNCHECKED_CAST")
            return this as T
        }
    }

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