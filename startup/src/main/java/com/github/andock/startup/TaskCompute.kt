package com.github.andock.startup

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile

class TaskCompute<T>(
    initializer: Initializer<T>
) {
    fun interface Initializer<T> {
        suspend fun invoke(): TimeMillisWithResult<T>
    }

    @Volatile
    private var value: Any = initializer

    private val mutex = Mutex()

    @Suppress("UNCHECKED_CAST")
    suspend operator fun invoke(): TimeMillisWithResult<T> {
        val v1 = value
        if (v1 !is Initializer<*>) {
            return v1 as TimeMillisWithResult<T>
        }
        return mutex.withLock {
            val v2 = value
            if (v2 !is Initializer<*>) {
                v2 as TimeMillisWithResult<T>
            } else {
                val typedValue = (v2 as Initializer<T>).invoke()
                value = typedValue
                typedValue
            }
        }
    }
}