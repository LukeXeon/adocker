package com.github.andock.startup

import androidx.collection.MutableObjectLongMap
import androidx.collection.ObjectLongMap
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
) : Exception(key), suspend (CoroutineScope) -> ObjectLongMap<String>, Runnable {

    val key: String
        get() = message!!

    override fun fillInStackTrace(): Throwable {
        stackTrace = emptyArray()
        return this
    }

    override suspend fun invoke(scope: CoroutineScope): ObjectLongMap<String> {
        val tasks = tasks.getValue(key)
        val timeConsuming = MutableObjectLongMap<String>(tasks.size)
        tasks.map { (key, task) ->
            scope.async(Dispatchers.IO) {
                key to task.getValue()
            }
        }.awaitAll().forEach { (key, ms) ->
            timeConsuming[key] = ms
        }
        Dispatchers.Main.immediate.dispatch(
            EmptyCoroutineContext,
            this
        )
        return timeConsuming
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