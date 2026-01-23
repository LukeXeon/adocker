package com.github.andock.startup.coroutines

import com.github.andock.startup.coroutines.ContextElementInterceptor.Companion.intercept
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

internal class CombinedCoroutineContext(
    private val left: CoroutineContext,
    private val element: CoroutineContext.Element
) : AbstractCoroutineContext() {

    override fun <E : CoroutineContext.Element> get(key: CoroutineContext.Key<E>): E? {
        var cur = this
        while (true) {
            cur.element[key]?.let { return it }
            val next = cur.left
            if (next is CombinedCoroutineContext) {
                cur = next
            } else {
                return next[key]
            }
        }
    }

    override fun <R> fold(initial: R, operation: (R, CoroutineContext.Element) -> R): R =
        operation(left.fold(initial, operation), element)

    override fun minusKey(key: CoroutineContext.Key<*>): CoroutineContext {
        element[key]?.let { return left }
        val newLeft = left.minusKey(key)
        return when {
            newLeft === left -> this
            newLeft === EmptyCoroutineContext -> element
            else -> CombinedCoroutineContext(newLeft, element)
        }
    }

    private fun size(): Int {
        var cur = this
        var size = 2
        while (true) {
            cur = cur.left as? CombinedCoroutineContext ?: return size
            size++
        }
    }

    private fun contains(element: CoroutineContext.Element): Boolean =
        get(element.key) == element

    private fun containsAll(context: CombinedCoroutineContext): Boolean {
        var cur = context
        while (true) {
            if (!contains(cur.element)) return false
            val next = cur.left
            if (next is CombinedCoroutineContext) {
                cur = next
            } else {
                return contains(next as CoroutineContext.Element)
            }
        }
    }

    private fun combine(context: CoroutineContext): CoroutineContext {
        return super.plus(context)
    }

    override fun plus(context: CoroutineContext): CoroutineContext {
        return fold(combine(context)) { acc, element ->
            if (acc is CombinedCoroutineContext && element is ContextElementInterceptor<*>) {
                val element = element.intercept(acc)
                if (element != null) {
                    return@fold acc.combine(element)
                }
            }
            return@fold acc
        }
    }

    override fun equals(other: Any?): Boolean =
        this === other || other is CombinedCoroutineContext
                && other.size() == size()
                && other.containsAll(this)

    override fun hashCode(): Int = left.hashCode() + element.hashCode()

    override fun toString(): String =
        "[" + fold("") { acc, element ->
            if (acc.isEmpty()) element.toString() else "$acc, $element"
        } + "]"

}