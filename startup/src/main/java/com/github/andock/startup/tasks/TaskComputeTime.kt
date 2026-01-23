package com.github.andock.startup.tasks

import dagger.Lazy

class TaskComputeTime(
    private val task: Lazy<out TaskCompute<*>>
) {
    suspend operator fun invoke(): Long {
        return task.get().invoke().timeMillis
    }
}