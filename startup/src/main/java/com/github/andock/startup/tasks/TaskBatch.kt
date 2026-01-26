package com.github.andock.startup.tasks

import com.github.andock.startup.InternalName
import com.github.andock.startup.coroutines.MainDispatcherInterceptor
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
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
    private var isEntered: Boolean = false

    override suspend fun invoke(scope: CoroutineScope): List<TaskResult> {
        if (isEntered) {
            val tasks = tasks.getValue(key)
            return tasks.map { task ->
                with(task) {
                    scope.start()
                }
            }.awaitAll()
        } else {
            try {
                isEntered = true
                val mainDispatcher = checkNotNull(scope.coroutineContext[CoroutineDispatcher]) {
                    "not found CoroutineDispatcher"
                }
                return withContext(
                    context = MainDispatcherInterceptor(mainDispatcher) + Dispatchers.Main.immediate,
                    block = this
                )
            } finally {
                isEntered = false
            }
        }
    }

    @Singleton
    @AssistedFactory
    interface NewInstance {
        operator fun invoke(
            @Assisted key: String
        ): TaskBatch
    }
}