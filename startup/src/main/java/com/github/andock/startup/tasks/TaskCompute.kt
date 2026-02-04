package com.github.andock.startup.tasks

import com.github.andock.common.TimeMillisWithResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile

class TaskCompute<T> @PublishedApi internal constructor(
    initializer: Initializer<T>
) {

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
                val typedValue = (v2 as Initializer<T>)()
                value = typedValue
                typedValue
            }
        }
    }

    companion object {
        inline operator fun <T> invoke(crossinline block: suspend () -> TimeMillisWithResult<T>): TaskCompute<T> {
            return TaskCompute { block() }
        }
    }
}