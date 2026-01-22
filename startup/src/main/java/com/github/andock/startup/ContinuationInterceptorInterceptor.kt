package com.github.andock.startup

import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

interface ContinuationInterceptorInterceptor : CoroutineContext.Element {

    fun intercept(interceptor: ContinuationInterceptor): ContinuationInterceptor

    companion object Key : CoroutineContext.Key<ContinuationInterceptorInterceptor>
}