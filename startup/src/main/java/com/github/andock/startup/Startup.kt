package com.github.andock.startup

import android.content.Context
import android.os.Looper
import androidx.annotation.MainThread
import com.github.andock.startup.coroutines.RootContext
import com.github.andock.startup.tasks.TaskBatchFactory
import com.github.andock.startup.tasks.TaskResult
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking


@OptIn(DelicateCoroutinesApi::class)
@MainThread
fun Context.trigger(
    key: String,
): List<TaskResult> {
    require(Looper.getMainLooper().isCurrentThread) { "must be main thread" }
    val entrypoint = EntryPointAccessors.fromApplication<TaskBatchFactory>(this)
    val batch = entrypoint.newInstance(key)
    return runBlocking(
        context = RootContext(key),
        block = batch
    )
}

suspend fun triggerKey(): String {
    return requireNotNull(
        currentCoroutineContext()[RootContext]
    ) {
        "not found TriggerCoroutineContext"
    }.triggerKey
}