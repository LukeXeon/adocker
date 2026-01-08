package com.github.andock.startup

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import javax.inject.Singleton
import kotlin.coroutines.EmptyCoroutineContext

internal class TaskBatch @AssistedInject constructor(
    @Assisted
    key: String,
    private val tasks: @JvmSuppressWildcards Map<String, Map<String, SuspendLazy<Long>>>,
) : Exception(key), suspend (CoroutineScope) -> ArrayList<TaskResult>, Runnable {

    val key: String
        get() = message!!

    override fun fillInStackTrace(): Throwable {
        stackTrace = emptyArray()
        return this
    }

    override suspend fun invoke(scope: CoroutineScope): ArrayList<TaskResult> {
        val tasks = tasks.getValue(key)
        val results = ArrayList<TaskResult>(tasks.size)
        tasks.map { (key, task) ->
            scope.async(Dispatchers.IO) {
                key to task.getValue()
            }
        }.awaitAll().forEach { (key, ms) ->
            results.add(TaskResult(key, ms, false))
        }
        Dispatchers.Main.immediate.dispatch(
            EmptyCoroutineContext,
            this
        )
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