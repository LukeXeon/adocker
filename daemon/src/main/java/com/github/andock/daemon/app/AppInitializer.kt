package com.github.andock.daemon.app

import android.os.Looper
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(DelicateCoroutinesApi::class)
@Singleton
class AppInitializer @Inject constructor(
    private val tasks: Set<@JvmSuppressWildcards Task<*>>
) {
    private val called = AtomicBoolean(false)
    fun onCreate() {
        require(Looper.getMainLooper().isCurrentThread) { "must be main thread" }
        if (called.compareAndSet(false, true)) {
            val jumpOutException = object : RuntimeException() {
                override fun fillInStackTrace(): Throwable {
                    stackTrace = emptyArray()
                    return this
                }
            }
            GlobalScope.launch(Dispatchers.Main) {
                yield()
                tasks.map {
                    launch {
                        it.getValue()
                    }
                }.joinAll()
                Dispatchers.Main.dispatch(EmptyCoroutineContext) {
                    throw jumpOutException
                }
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
        protected abstract fun create(): T
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
