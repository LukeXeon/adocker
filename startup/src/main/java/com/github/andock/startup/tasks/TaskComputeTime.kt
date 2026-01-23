package com.github.andock.startup.tasks

import com.github.andock.startup.utils.measureTimeMillis
import dagger.Lazy

class TaskComputeTime(
    private val task: Lazy<out TaskCompute<*>>
) {
    suspend operator fun invoke(): LongArray {
        val times = LongArray(2)
        times[1] = measureTimeMillis {
            times[0] = task.get().invoke().timeMillis
        }
        return times
    }
}