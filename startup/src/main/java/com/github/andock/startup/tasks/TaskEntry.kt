package com.github.andock.startup.tasks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

data class TaskEntry(
    val name: String,
    val compute: TaskComputeTime
) : suspend (CoroutineScope) -> TaskResult {

    override suspend fun invoke(scope: CoroutineScope): TaskResult {
        val (phaseTime, totalTime) = compute()
        return TaskResult(name, phaseTime, totalTime)
    }

    fun start(scope: CoroutineScope): Deferred<TaskResult> {
        return scope.async(block = this)
    }
}