package com.github.andock.startup

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Provider

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
    };
}