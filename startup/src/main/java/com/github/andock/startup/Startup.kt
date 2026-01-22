package com.github.andock.startup

import android.content.Context
import android.os.Looper
import androidx.annotation.MainThread
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
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
    runBlocking(
        context = RootCoroutineContext
    ) {
        withContext(
            context = RedirectDispatchers(
                thread = Thread.currentThread(),
                dispatcher = coroutineContext[CoroutineDispatcher]!!
            ),
            block = batch
        )

    }
    return runBlocking(block = batch)
}