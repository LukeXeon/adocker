package com.github.andock.startup

import android.content.Context
import android.os.Looper
import androidx.annotation.MainThread
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.runBlocking

@OptIn(DelicateCoroutinesApi::class)
@MainThread
fun Context.trigger(
    key: String = "",
): List<TaskResult> {
    require(Looper.getMainLooper().isCurrentThread) { "must be main thread" }
    val entrypoint = EntryPointAccessors.fromApplication<TaskBatchFactory>(this)
    val batch = entrypoint.newInstance(key)
    return runBlocking(
        context = StartupRootContext,
        block = batch
    )
}