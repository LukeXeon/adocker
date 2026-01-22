package com.github.andock.startup


import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.CoroutineContext.Element
import kotlin.coroutines.CoroutineContext.Key


internal object RootCoroutineContext : CoroutineContext {
    override fun <E : Element> get(key: Key<E>): E? = null
    override fun <R> fold(initial: R, operation: (R, Element) -> R): R = initial
    override fun minusKey(key: Key<*>): CoroutineContext = this
    override fun hashCode(): Int = 0
    override fun toString(): String = "RootCoroutineContext"
    override fun plus(context: CoroutineContext): CoroutineContext {
        return CombinedContext.combine(this, context)
    }
}