package com.github.andock.startup

import android.content.Context
import android.os.Looper
import androidx.annotation.MainThread
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

@OptIn(
    DelicateCoroutinesApi::class,
    ExperimentalCoroutinesApi::class
)
@MainThread
fun Context.trigger(
    key: String = "",
): List<TaskResult> {
    val (results, ms) = measureTimeMillisWithResult {
        require(Looper.getMainLooper().isCurrentThread) { "must be main thread" }
        val entrypoint = EntryPointAccessors.fromApplication<StartupEntryPoint>(this)
        val batch = entrypoint.factory.create(key)
        val async = GlobalScope.async(
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
        async.getCompleted()
    }
    results.add(TaskResult("trigger:${key}", longArrayOf(ms, ms)))
    return results
}