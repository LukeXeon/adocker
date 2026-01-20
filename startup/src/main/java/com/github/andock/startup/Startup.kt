package com.github.andock.startup

import android.content.Context
import android.os.Looper
import androidx.annotation.MainThread
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture

@OptIn(DelicateCoroutinesApi::class)
@MainThread
fun Context.trigger(
    key: String = "",
): List<TaskResult> {
    require(Looper.getMainLooper().isCurrentThread) { "must be main thread" }
    val entrypoint = EntryPointAccessors.fromApplication<TaskBatchFactory>(this)
    val batch = entrypoint.newInstance(key)
    val job = GlobalScope.async(
        context = Dispatchers.Main.immediate,
        start = CoroutineStart.UNDISPATCHED,
        block = batch
    )
    try {
        Looper.loop()
    } catch (e: TaskBatch) {
        if (e != batch) {
            throw e
        }
    }
    return job.asCompletableFuture().get()
}