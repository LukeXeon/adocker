package com.github.andock.startup

import android.content.Context
import android.os.Looper
import androidx.annotation.MainThread
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@OptIn(DelicateCoroutinesApi::class)
@MainThread
fun Context.trigger(
    key: String = "",
): List<TaskResult> {
    require(Looper.getMainLooper().isCurrentThread) { "must be main thread" }
    val entrypoint = EntryPointAccessors.fromApplication<TaskBatchFactory>(this)
    val batch = entrypoint.newInstance(key)
    return runBlocking(
        context = StartupRootContext
    ) {
        withContext(
            context = RedirectDispatchers(
                mainDispatcher = coroutineContext[CoroutineDispatcher]!!
            ) + Dispatchers.Main.immediate,
            block = batch
        )
    }
}