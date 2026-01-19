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
    private val tasks: @JvmSuppressWildcards Map<String, Map<String, TaskComputeTime>>,
) : Exception(key), suspend (CoroutineScope) -> ArrayList<TaskResult>, Runnable {

    val key: String
        get() = message!!

    override fun fillInStackTrace(): Throwable {
        stackTrace = emptyArray()
        return this
    }

    override suspend fun invoke(scope: CoroutineScope): ArrayList<TaskResult> {
        val tasks = tasks.getValue(key)
        val results = ArrayList<TaskResult>(tasks.size + 1)
        tasks.map { (key, task) ->
            scope.async(Dispatchers.IO) {
                key to task()
            }
        }.awaitAll().forEach { (key, times) ->
            results.add(TaskResult("task:$key", times))
        }
        mainThread.postAtFrontOfQueue(this)
        return results
    }

    override fun run() {
        throw this
    }

    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted key: String
        ): TaskBatch
    }
}