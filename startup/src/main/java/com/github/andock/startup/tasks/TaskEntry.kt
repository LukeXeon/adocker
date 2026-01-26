package com.github.andock.startup.tasks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

data class TaskEntry(
    val name: String,
    val compute: TaskComputeTime
) : suspend (CoroutineScope) -> TaskResult {

    override suspend fun invoke(scope: CoroutineScope): TaskResult {
        return TaskResult(name, compute())
    }

    fun CoroutineScope.start(): Deferred<TaskResult> {
        return async(block = this@TaskEntry)
    }
}