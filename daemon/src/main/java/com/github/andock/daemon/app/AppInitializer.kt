package com.github.andock.daemon.app

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInitializer @Inject constructor(
    private val tasks: Set<@JvmSuppressWildcards Task<*>>,
    private val scope: CoroutineScope,
) {
    private val called = AtomicBoolean(false)
    fun onCreate() {
        val mainLooper = Looper.getMainLooper()
        require(mainLooper.isCurrentThread) { "must be main thread" }
        if (called.compareAndSet(false, true)) {
            val jumpOutException = object : RuntimeException(), Runnable {
                override fun fillInStackTrace(): Throwable {
                    stackTrace = emptyArray()
                    return this
                }

                override fun run() {
                    throw this
                }
            }
            val mainHandler = Handler(mainLooper)
            scope.launch(mainHandler.asCoroutineDispatcher()) {
                tasks.map {
                    launch {
                        it.getValue()
                    }
                }.joinAll()
                mainHandler.postAtFrontOfQueue(jumpOutException)
            }
            try {
                Looper.loop()
            } catch (e: Throwable) {
                if (jumpOutException != e) {
                    throw e
                }
            }
        }
    }

    private object UninitializedValue

    abstract class Task<T> {
        protected abstract suspend fun create(): T
        private val lock = Mutex()

        @Volatile
        private var value: Any? = UninitializedValue

        suspend fun getValue(): T {
            val v1 = value
            if (v1 !== UninitializedValue) {
                @Suppress("UNCHECKED_CAST")
                return v1 as T
            }
            return lock.withLock {
                val v2 = value
                if (v2 !== UninitializedValue) {
                    @Suppress("UNCHECKED_CAST") (v2 as T)
                } else {
                    val typedValue = create()
                    value = typedValue
                    typedValue
                }
            }
        }
    }
}
