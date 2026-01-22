package com.github.andock.startup

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import javax.inject.Singleton

internal class TaskBatch @AssistedInject constructor(
    @Assisted
    private val key: String,
    @param:InternalName("tasks")
    private val tasks: @JvmSuppressWildcards Map<String, List<Pair<String, TaskComputeTime>>>,
) : suspend (CoroutineScope) -> List<TaskResult> {
    private var isEntered: Boolean = false

    override suspend fun invoke(scope: CoroutineScope): List<TaskResult> {
        if (isEntered) {
            val tasks = tasks.getValue(key)
            return tasks.map { task ->
                scope.async(Dispatchers.Default) {
                    val (phaseTime, totalTime) = task.second()
                    TaskResult(task.first, phaseTime, totalTime)
                }
            }.awaitAll()
        } else {
            try {
                isEntered = true
                val context = currentCoroutineContext()
                val mainDispatcher = checkNotNull(context[CoroutineDispatcher]) {
                    "not found CoroutineDispatcher"
                }
                return withContext(
                    context = RedirectDispatchers(mainDispatcher) + Dispatchers.Main.immediate,
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