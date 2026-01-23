package com.github.andock.startup.coroutines


import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.CoroutineContext.Element
import kotlin.coroutines.CoroutineContext.Key


internal object RootCoroutineContext : AbstractCoroutineContext() {
    override fun <E : Element> get(key: Key<E>): E? = null
    override fun <R> fold(initial: R, operation: (R, Element) -> R): R = initial
    override fun minusKey(key: Key<*>): CoroutineContext = this
    override fun hashCode(): Int = 0
    override fun toString(): String = javaClass.name
}