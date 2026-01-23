package com.github.andock.startup

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Provider

enum class TaskDispatchers : Provider<CoroutineDispatcher> {
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
        override fun get() = Dispatchers.ForkJoin
    },
    AsyncTask {
        override fun get() = Dispatchers.AsyncTask
    },
    QueuedWork {
        override fun get() = Dispatchers.QueuedWork
    }
}