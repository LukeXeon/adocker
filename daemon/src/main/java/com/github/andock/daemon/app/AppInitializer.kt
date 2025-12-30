package com.github.andock.daemon.app

import android.os.Handler
import android.os.Looper
import com.github.andock.daemon.utils.SuspendLazy
import com.github.andock.daemon.utils.measureTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInitializer @Inject constructor(
    private val tasks: Map<String, @JvmSuppressWildcards SuspendLazy<*>>,
    private val scope: CoroutineScope,
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

    fun onCreate() {
        val mainLooper = Looper.getMainLooper()
        require(mainLooper.isCurrentThread) { "must be main thread" }
        if (called.compareAndSet(false, true)) {
            val jumpOutException = JumpOutException()
            val mainHandler = Handler(mainLooper)
            scope.launch(mainHandler.asCoroutineDispatcher().immediate) {
                val ms = measureTimeMillis {
                    tasks.map { (key, task) ->
                        async {
                            key to measureTimeMillis {
                                task.getValue()
                            }
                        }
                    }.awaitAll().forEach { (key, ms) ->
                        Timber.d("task ${key}: ${ms}ms")
                    }
                }
                Timber.d("task all: ${ms}ms")
                mainHandler.postAtFrontOfQueue(jumpOutException)
            }
            try {
                Looper.loop()
            } catch (e: JumpOutException) {
                Timber.d(e)
            }
        }
    }
}
