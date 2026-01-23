@file:Suppress("DEPRECATION")

package com.github.andock.startup

import android.os.AsyncTask
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.ForkJoinPool


private const val TAG = "ExtraDispatchers"

val Dispatchers.QueuedWork by lazy {
    runCatching {
        Class.forName("android.os.QueuedWork")
            .getDeclaredMethod("getHandler").apply {
                isAccessible = true
            }.invoke(null) as Handler
    }.map {
        it.looper
    }.recover {
        Log.e(TAG, "access android.os.QueuedWork failed", it)
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

val Dispatchers.AsyncTask by lazy {
    AsyncTask.THREAD_POOL_EXECUTOR.asCoroutineDispatcher()
}

val Dispatchers.ForkJoin by lazy {
    ForkJoinPool.commonPool().asCoroutineDispatcher()
}