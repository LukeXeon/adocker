package com.github.andock.startup.coroutines

import kotlin.coroutines.CoroutineContext

internal class RootContext(
    val triggerKey: String
) : AbstractCoroutineContext(), CoroutineContext.Element {

    override val key: CoroutineContext.Key<*>
        get() = Key

    companion object Key : CoroutineContext.Key<RootContext>

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RootContext

        return triggerKey == other.triggerKey
    }

    override fun hashCode(): Int {
        return triggerKey.hashCode()
    }

    override fun toString(): String {
        return "RootContext(name='$triggerKey')"
    }
}