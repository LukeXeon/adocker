package com.github.andock.daemon.app

import android.os.Looper
import com.github.andock.daemon.utils.SuspendLazy
import com.github.andock.daemon.utils.measureTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(DelicateCoroutinesApi::class)
@Singleton
class AppInitializer @Inject constructor(
    rawTasks: Map<AppTaskInfo, @JvmSuppressWildcards SuspendLazy<Pair<*, Long>>>,
) {
    private val tasks: Map<String, Map<String, suspend () -> Long>>

    init {
        val map = mutableMapOf<String, MutableMap<String, suspend () -> Long>>()
        rawTasks.forEach { (key, value) ->
            map.getOrPut(key.trigger) { mutableMapOf() }[key.name] = {
                value.getValue().second
            }
        }
        tasks = map
    }

    private inner class TaskBatch(
        key: String
    ) : Exception(key), suspend (CoroutineScope) -> Unit, Runnable {
        val key: String
            get() = message!!

        override fun fillInStackTrace(): Throwable {
            stackTrace = emptyArray()
            return this
        }

        override fun run() {
            throw this
        }

        override suspend fun invoke(scope: CoroutineScope) {
            tasks.getValue(key)
                .map { (key, task) ->
                    scope.async {
                        key to task()
                    }
                }.awaitAll().forEach { (key, ms) ->
                    Timber.d("task ${key}: ${ms}ms")
                }
            Dispatchers.Main.immediate.dispatch(
                EmptyCoroutineContext,
                this
            )
        }
    }

    fun trigger(key: String = "") {
        require(Looper.getMainLooper().isCurrentThread) { "must be main thread" }
        val batch = TaskBatch(key)
        GlobalScope.launch(Dispatchers.IO, block = batch)
        val ms = measureTimeMillis {
            try {
                Looper.loop()
            } catch (e: TaskBatch) {
                if (e != batch) {
                    throw e
                }
            }
        }
        Timber.d("trigger: $key task all: ${ms}ms")
    }
}
