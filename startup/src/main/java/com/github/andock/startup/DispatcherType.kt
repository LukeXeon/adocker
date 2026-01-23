package com.github.andock.startup

import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.ForkJoinPool
import javax.inject.Provider
import android.os.AsyncTask as AndroidAsyncTask

enum class DispatcherType : Provider<CoroutineDispatcher> {
    Main {
        override fun get() = Dispatchers.Main
    },
    MainImmediate {
        override fun get() = Dispatchers.Main.immediate
    },
    Default {
        override fun get() = Dispatchers.Default
    },
    IO {
        override fun get() = Dispatchers.IO
    },
    ForkJoin {
        private val value by lazy {
            ForkJoinPool.commonPool().asCoroutineDispatcher()
        }

        override fun get(): CoroutineDispatcher {
            return value
        }
    },
    AsyncTask {
        private val value by lazy {
            AndroidAsyncTask.THREAD_POOL_EXECUTOR.asCoroutineDispatcher()
        }

        override fun get(): CoroutineDispatcher {
            return value
        }
    },
    QueuedWork {
        private val value by lazy {
            runCatching {
                Class.forName("android.os.QueuedWork")
                    .getDeclaredMethod("getHandler").apply {
                        isAccessible = true
                    }.invoke(null) as Handler
            }.map {
                it.looper
            }.recover {
                HandlerThread(
                    "queued-work-looper",
                    Process.THREAD_PRIORITY_FOREGROUND
                ).apply {
                    start()
                }.looper
            }.map {
                Handler(it).asCoroutineDispatcher()
            }.getOrThrow()
        }

        override fun get(): CoroutineDispatcher {
            return value
        }
    }
}