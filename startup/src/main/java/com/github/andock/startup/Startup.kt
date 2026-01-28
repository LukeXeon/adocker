package com.github.andock.startup

import android.content.Context
import android.os.Looper
import androidx.annotation.MainThread
import androidx.startup.AppInitializer
import com.github.andock.startup.coroutines.RootContext
import com.github.andock.startup.tasks.TaskBatch
import com.github.andock.startup.tasks.TaskResult
import com.github.andock.startup.tasks.measureTimeMillis
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking


@OptIn(DelicateCoroutinesApi::class)
@MainThread
fun Context.trigger(
    key: String,
): Stats {
    val tasks: List<TaskResult>
    val ms = measureTimeMillis {
        require(Looper.getMainLooper().isCurrentThread) { "must be main thread" }
        val entrypoint = EntryPointAccessors.fromApplication<TaskBatch.Factory>(this)
        val batch = entrypoint.newInstance(key)
        tasks = runBlocking(
            context = RootContext(key),
            block = batch
        )
    }
    return Stats(key, ms, tasks)
}

suspend fun triggerKey(): String {
    return requireNotNull(
        currentCoroutineContext()[RootContext]
    ) {
        "not found RootContext"
    }.name
}

val Context.stats: Stats
    get() {
        return AppInitializer.getInstance(this)
            .initializeComponent(AndroidXTrigger::class.java)
    }