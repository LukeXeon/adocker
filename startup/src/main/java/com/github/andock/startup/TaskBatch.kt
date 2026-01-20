package com.github.andock.startup

import android.os.Handler
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import javax.inject.Named
import javax.inject.Singleton

internal class TaskBatch @AssistedInject constructor(
    @Assisted
    key: String,
    @param:Named("main-thread")
    private val mainThread: Handler,
    private val tasks: @JvmSuppressWildcards Map<String, List<TaskComputeTime>>,
) : Exception(key), suspend (CoroutineScope) -> List<TaskResult>, Runnable {

    val key: String
        get() = message!!

    override fun fillInStackTrace(): Throwable {
        stackTrace = emptyArray()
        return this
    }

    override suspend fun invoke(scope: CoroutineScope): List<TaskResult> {
        try {
            return tasks.getValue(key).map { task ->
                scope.async(Dispatchers.Default) {
                    key to task()
                }
            }.awaitAll().asSequence().map { (key, times) ->
                TaskResult(key, times[0], times[1])
            }.toCollection(ArrayList(tasks.size))
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