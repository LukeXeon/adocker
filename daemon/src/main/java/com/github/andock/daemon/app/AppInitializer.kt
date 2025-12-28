package com.github.andock.daemon.app

import android.os.Handler
import android.os.Looper
import com.github.andock.daemon.utils.SuspendLazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInitializer @Inject constructor(
    private val tasks: Set<@JvmSuppressWildcards SuspendLazy<*>>,
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
                tasks.map {
                    launch {
                        it.getValue()
                    }
                }.joinAll()
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
