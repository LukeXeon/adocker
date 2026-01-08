package com.github.andock.daemon.containers.shell

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.github.andock.daemon.database.dao.InMemoryLogDao
import com.github.andock.daemon.database.model.InMemoryLogEntity
import com.github.andock.daemon.os.id
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
import javax.inject.Singleton


class ContainerShell @AssistedInject constructor(
    @Assisted
    process: Process,
    parent: CoroutineScope,
    factory: ContainerShellStateMachine.Factory,
    private val inMemoryLogStore: InMemoryLogDao,
) {
    companion object {
        private const val N = 1000
    }

    private val mutex = Mutex()
    private val writer = process.outputStream.bufferedWriter()

    private val scope = CoroutineScope(
        SupervisorJob(parent.coroutineContext[Job]) + Dispatchers.IO
    )

    private val stateMachine = factory.create(process).launchIn(scope)

    val state
        get() = stateMachine.state

    val id
        get() = state.value.process.id

    val logLines
        get() = Pager(
            config = PagingConfig(
                pageSize = N,
                enablePlaceholders = false,
                initialLoadSize = N,
            ),
            initialKey = 1,
            pagingSourceFactory = {
                inMemoryLogStore.getAllAsPaging(sessionId = id)
            }
        ).flow

    suspend fun stop() {
        stateMachine.dispatch(Unit)
    }

    suspend fun exec(command: String) {
        inMemoryLogStore.append(
            InMemoryLogEntity(
                id = 0,
                timestamp = System.currentTimeMillis(),
                sessionId = id,
                message = command
            )
        )
        mutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    writer.appendLine(command)
                } finally {
                    writer.flush()
                }
            }
        }
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