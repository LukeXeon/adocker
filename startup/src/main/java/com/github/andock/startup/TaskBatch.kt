package com.github.andock.startup

import android.os.Handler
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import javax.inject.Singleton

internal class TaskBatch @AssistedInject constructor(
    @Assisted
    key: String,
    @param:Internal
    private val mainThread: Handler,
    @param:Internal
    private val tasks: @JvmSuppressWildcards Map<String, List<Pair<String, TaskComputeTime>>>,
) : Exception(key), suspend (CoroutineScope) -> List<TaskResult>, Runnable {

    val key: String
        get() = message!!

    override fun fillInStackTrace(): Throwable {
        stackTrace = emptyArray()
        return this
    }

    override suspend fun invoke(scope: CoroutineScope): List<TaskResult> {
        try {
            val tasks = tasks.getValue(key)
            return tasks.map { task ->
                scope.async(Dispatchers.Default) {
                    val (phaseTime, totalTime) = task.second()
                    TaskResult(task.first, phaseTime, totalTime)
                }
            }.awaitAll()
        } finally {
            mainThread.postAtFrontOfQueue(this)
        }
    }

    override fun run() {
        throw this
    }

    @Singleton
    @AssistedFactory
    interface NewInstance {
        operator fun invoke(
            @Assisted key: String
        ): TaskBatch
    }
}