package com.github.andock.startup

import android.content.Context
import android.os.Looper
import androidx.annotation.MainThread
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import timber.log.Timber

@OptIn(
    DelicateCoroutinesApi::class,
    ExperimentalCoroutinesApi::class
)
@MainThread
fun Context.trigger(
    key: String = "",
    timber: Timber.Tree = Timber
) {
    require(Looper.getMainLooper().isCurrentThread) { "must be main thread" }
    val entrypoint = EntryPointAccessors.fromApplication<StartupEntryPoint>(this)
    val batch = entrypoint.factory.create(key)
    val async = GlobalScope.async(Dispatchers.Main.immediate, block = batch)
    val ms = measureTimeMillis {
        try {
            Looper.loop()
        } catch (e: TaskBatch) {
            if (e != batch) {
                throw e
            }
        }
    }
    val tasks = async.getCompleted()
    tasks.forEach { key, ms ->
        timber.i("task $key: ${ms}ms")
    }
    timber.i("trigger: ${key.ifEmpty { "default" }}, task all: ${ms}ms")
}