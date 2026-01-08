package com.github.andock.daemon.containers.shell

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.github.andock.daemon.database.dao.InMemoryLogDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Singleton


class ContainerShell @AssistedInject constructor(
    @Assisted
    process: Process,
    parent: CoroutineScope,
    stateMachineFactory: ContainerShellStateMachine.Factory,
    private val inMemoryLogStore: InMemoryLogDao,
) {
    companion object {
        private const val N = 1000
    }

    private val mutex = Mutex()
    private val writer = process.outputStream.writer()

    private val scope = CoroutineScope(
        SupervisorJob(parent.coroutineContext[Job]) + Dispatchers.IO
    )

    private val stateMachine = stateMachineFactory.create(process).launchIn(scope)

    val state
        get() = stateMachine.state

    val id
        get() = state.value.id

    val logLines
        get() = Pager(
            config = PagingConfig(
                pageSize = N,
                enablePlaceholders = false,
                initialLoadSize = N,
            ),
            initialKey = 1,
            pagingSourceFactory = {
                inMemoryLogStore.getLogLinesPaged(sessionId = id)
            }
        ).flow

    suspend fun exec(command: String) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                writer.appendLine(command)
            }
        }
        Timber.e(command)
    }

    init {
        scope.launch {
            stateMachine.state.collect {
                if (it is ContainerShellState.Exited) {
                    scope.cancel()
                }
            }
        }
    }

    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted
            process: Process
        ): ContainerShell
    }
}