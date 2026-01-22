package com.github.andock.startup

import android.os.Looper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Delay
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlin.coroutines.CoroutineContext

@OptIn(InternalCoroutinesApi::class)
internal class EventLoopMainCoroutineDispatcher(
    private val dispatcher: CoroutineDispatcher,
    private val invokeImmediately: Boolean = false
) : MainCoroutineDispatcher(), Delay by dispatcher as Delay {
    override val immediate: MainCoroutineDispatcher = if (invokeImmediately) {
        this
    } else {
        EventLoopMainCoroutineDispatcher(dispatcher, true)
    }

    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        return !invokeImmediately || !Looper.getMainLooper().isCurrentThread
    }

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable
    ) {
        dispatcher.dispatch(context, block)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EventLoopMainCoroutineDispatcher

        if (invokeImmediately != other.invokeImmediately) return false
        if (dispatcher != other.dispatcher) return false
        return true
    }

    override fun hashCode(): Int {
        var result = invokeImmediately.hashCode()
        result = 31 * result + dispatcher.hashCode()
        return result
    }
}