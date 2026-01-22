package com.github.andock.startup

import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

interface StartupCoroutineContext : CoroutineContext {
    override fun plus(context: CoroutineContext): CoroutineContext {
        return if (context === EmptyCoroutineContext) {
            // fast path -- avoid lambda creation
            this
        } else {
            context.fold<CoroutineContext>(this) { acc, element ->
                val removed = acc.minusKey(element.key)
                if (removed === EmptyCoroutineContext) {
                    element
                } else {
                    // make sure interceptor is always last in the context (and thus is fast to get when present)
                    val interceptor = removed[ContinuationInterceptor]
                    if (interceptor == null) {
                        StartupCombinedContext(removed, element)
                    } else {
                        val left = removed.minusKey(ContinuationInterceptor)
                        if (left === EmptyCoroutineContext) {
                            StartupCombinedContext(element, interceptor)
                        } else {
                            StartupCombinedContext(
                                StartupCombinedContext(left, element),
                                interceptor
                            )
                        }
                    }
                }
            }
        }
    }
}