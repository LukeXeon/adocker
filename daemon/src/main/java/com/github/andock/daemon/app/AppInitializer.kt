package com.github.andock.daemon.app

import android.os.Looper
import com.github.andock.daemon.utils.SuspendLazy
import com.github.andock.daemon.utils.measureTimeMillis
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(DelicateCoroutinesApi::class)
@Singleton
class AppInitializer @Inject constructor(
    rawTasks: Map<String, @JvmSuppressWildcards SuspendLazy<Pair<*, Long>>>,
    json: Json,
) {
    private val tasks: Map<String, Map<String, suspend () -> Long>>

    init {
        val map = mutableMapOf<String, MutableMap<String, suspend () -> Long>>()
        rawTasks.forEach { (key, value) ->
            val info = json.decodeFromString<AppTaskInfo>(key)
            map.getOrPut(info.trigger) { mutableMapOf() }[info.name] = {
                value.getValue().second
            }
        }
        tasks = map
    }

    private class JumpOutException : RuntimeException("Jump out"), Runnable {
        override fun fillInStackTrace(): Throwable {
            stackTrace = emptyArray()
            return this
        }

        override fun run() {
            throw this
        }
    }

    fun trigger(key: String = "") {
        val mainLooper = Looper.getMainLooper()
        require(mainLooper.isCurrentThread) { "must be main thread" }
        val jumpOutException = JumpOutException()
        GlobalScope.launch(Dispatchers.Main.immediate) {
            tasks.getValue(key)
                .map { (key, task) ->
                    async {
                        key to task()
                    }
                }.awaitAll().forEach { (key, ms) ->
                    Timber.d("task ${key}: ${ms}ms")
                }
            Dispatchers.Main.immediate.dispatch(
                EmptyCoroutineContext,
                jumpOutException
            )
        }
        val ms = measureTimeMillis {
            try {
                Looper.loop()
            } catch (e: JumpOutException) {
                if (e != jumpOutException) {
                    throw e
                }
            }
        }
        Timber.d("trigger: $key task all: ${ms}ms")
    }
}
