package com.github.andock.startup

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile

class TaskCompute<T>(
    initializer: suspend () -> TimeMillisWithResult<T>
) {
    private var initializer: (suspend () -> TimeMillisWithResult<T>)? = initializer

    @Volatile
    private var value: TimeMillisWithResult<T>? = null

    private val mutex = Mutex()

    suspend operator fun invoke(): TimeMillisWithResult<T> {
        val v1 = value
        if (v1 != null) {
            return v1
        }
        return mutex.withLock {
            val v2 = value
            if (v2 != null) {
                v2
            } else {
                val typedValue = initializer!!.invoke()
                value = typedValue
                initializer = null
                typedValue
            }
        }
    }
}