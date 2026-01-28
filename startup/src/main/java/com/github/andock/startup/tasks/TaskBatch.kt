package com.github.andock.startup.tasks

import com.github.andock.startup.InternalName
import com.github.andock.startup.coroutines.MainDispatcherInterceptor
import com.github.andock.startup.coroutines.interceptor
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Singleton

internal class TaskBatch @AssistedInject constructor(
    @Assisted
    private val key: String,
    @param:InternalName("tasks")
    private val tasks: @JvmSuppressWildcards Map<String, List<TaskEntry>>,
) : suspend (CoroutineScope) -> List<TaskResult> {

    override suspend fun invoke(scope: CoroutineScope): List<TaskResult> {
        val interceptor = scope.coroutineContext[CoroutineDispatcher.interceptor()]
        if (interceptor != null) {
            val tasks = tasks.getValue(key)
            return tasks.map { task ->
                with(task) {
                    scope.start()
                }
            }.awaitAll()
        } else {
            val dispatcher = checkNotNull(scope.coroutineContext[CoroutineDispatcher]) {
                "not found CoroutineDispatcher"
            }
            return withContext(
                context = MainDispatcherInterceptor(dispatcher) + Dispatchers.Main.immediate,
                block = this
            )
        }
    }

    @Singleton
    @AssistedFactory
    interface NewInstance {
        operator fun invoke(
            @Assisted key: String
        ): TaskBatch
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface Factory {
        val newInstance: NewInstance
    }
}