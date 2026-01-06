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
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(DelicateCoroutinesApi::class)
@Singleton
class AppInitializer @Inject constructor(
    private val tasks: Map<String, @JvmSuppressWildcards SuspendLazy<Pair<*, Long>>>,
) {
    private val called = AtomicBoolean(false)

    private class JumpOutException : RuntimeException("Jump out"), Runnable {
        override fun fillInStackTrace(): Throwable {
            stackTrace = emptyArray()
            return this
        }

        override fun run() {
            throw this
        }
    }

    fun install() {
        val mainLooper = Looper.getMainLooper()
        require(mainLooper.isCurrentThread) { "must be main thread" }
        if (called.compareAndSet(false, true)) {
            val jumpOutException = JumpOutException()
            GlobalScope.launch(Dispatchers.Main.immediate) {
                tasks.map { (key, task) ->
                    async {
                        key to task.getValue()
                    }
                }.awaitAll().forEach { (key, task) ->
                    Timber.d("task ${key}: ${task.second}ms")
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
                    Timber.d(e)
                }
            }
            Timber.d("task all: ${ms}ms")
        }
    }
}
