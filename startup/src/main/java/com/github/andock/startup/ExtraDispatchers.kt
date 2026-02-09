@file:Suppress("DEPRECATION")

package com.github.andock.startup

import android.os.AsyncTask
import android.os.Handler
import com.github.andock.common.queuedWorkLooper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.ForkJoinPool


val Dispatchers.QueuedWork by lazy {
    Handler(queuedWorkLooper).asCoroutineDispatcher()
}

val Dispatchers.AsyncTask by lazy {
    AsyncTask.THREAD_POOL_EXECUTOR.asCoroutineDispatcher()
}

val Dispatchers.ForkJoin by lazy {
    ForkJoinPool.commonPool().asCoroutineDispatcher()
}