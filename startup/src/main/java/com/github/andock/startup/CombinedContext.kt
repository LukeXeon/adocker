package com.github.andock.startup

import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

internal class CombinedContext(
    private val left: CoroutineContext,
    private val element: CoroutineContext.Element
) : CoroutineContext {

    override fun <E : CoroutineContext.Element> get(key: CoroutineContext.Key<E>): E? {
        var cur = this
        while (true) {
            cur.element[key]?.let { return it }
            val next = cur.left
            if (next is CombinedContext) {
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
            else -> CombinedContext(newLeft, element)
        }
    }

    private fun size(): Int {
        var cur = this
        var size = 2
        while (true) {
            cur = cur.left as? CombinedContext ?: return size
            size++
        }
    }

    private fun contains(element: CoroutineContext.Element): Boolean =
        get(element.key) == element

    private fun containsAll(context: CombinedContext): Boolean {
        var cur = context
        while (true) {
            if (!contains(cur.element)) return false
            val next = cur.left
            if (next is CombinedContext) {
                cur = next
            } else {
                return contains(next as CoroutineContext.Element)
            }
        }
    }

    override fun plus(context: CoroutineContext): CoroutineContext {
        var interceptor = context[ContinuationInterceptor]
        val interceptorInterceptor = this[ContinuationInterceptorInterceptor]
        if (interceptor != null) {
            interceptor = interceptorInterceptor?.intercept(interceptor)
        }
        return if (interceptor != null) {
            combine(this, context + interceptor)
        } else {
            combine(this, context)
        }
    }

    override fun equals(other: Any?): Boolean =
        this === other || other is CombinedContext
                && other.size() == size()
                && other.containsAll(this)

    override fun hashCode(): Int = left.hashCode() + element.hashCode()

    override fun toString(): String =
        "[" + fold("") { acc, element ->
            if (acc.isEmpty()) element.toString() else "$acc, $element"
        } + "]"


    companion object {
        fun combine(context1: CoroutineContext, context2: CoroutineContext): CoroutineContext {
            return if (context1 === EmptyCoroutineContext) {
                // fast path -- avoid lambda creation
                context1
            } else {
                context2.fold(context1) { acc, element ->
                    val removed = acc.minusKey(element.key)
                    if (removed === EmptyCoroutineContext) element else {
                        // make sure interceptor is always last in the context (and thus is fast to get when present)
                        val interceptor = removed[ContinuationInterceptor]
                        if (interceptor == null) {
                            CombinedContext(removed, element)
                        } else {
                            val left = removed.minusKey(ContinuationInterceptor)
                            if (left === EmptyCoroutineContext) {
                                CombinedContext(element, interceptor)
                            } else {
                                CombinedContext(CombinedContext(left, element), interceptor)
                            }
                        }
                    }
                }
            }
        }
    }
}