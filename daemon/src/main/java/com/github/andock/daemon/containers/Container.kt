package com.github.andock.daemon.containers

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.github.andock.daemon.containers.shell.ContainerShell
import com.github.andock.daemon.database.dao.ContainerDao
import com.github.andock.daemon.database.dao.ContainerLogDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Singleton

class Container @AssistedInject constructor(
    @Assisted
    initialState: ContainerState,
    stateMachineFactory: ContainerStateMachine.Factory,
    parent: CoroutineScope,
    containerDao: ContainerDao,
    private val containerLogDao: ContainerLogDao,
    private val shellFactory: ContainerShell.Factory
) {
    companion object {
        const val N = 1000
    }

    init {
        require(initialState is ContainerState.Created || initialState is ContainerState.Exited)
    }

    private val scope = CoroutineScope(
        SupervisorJob(parent.coroutineContext[Job]) + Dispatchers.IO
    )
    private val stateMachine = stateMachineFactory.create(initialState).launchIn(scope)

    val id
        get() = state.value.id

    val state
        get() = stateMachine.state

    val metadata = containerDao.getContainerFlowById(id).stateIn(
        scope,
        SharingStarted.Eagerly,
        null
    )

    val logLines
        get() = Pager(
            config = PagingConfig(
                pageSize = N,
                enablePlaceholders = false,
                initialLoadSize = N,
            ),
            initialKey = 1,
            pagingSourceFactory = {
                containerLogDao.getLogLinesPaged(containerId = id)
            }
        ).flow


    init {
        scope.launch {
            stateMachine.state.collect {
                if (it is ContainerState.Removed) {
                    scope.cancel()
                }
            }
        }
    }

    suspend fun exec(command: List<String>): Result<Process> {
        return try {
            // TODO inState
            val process = CompletableDeferred<Process>()
            stateMachine.dispatch(
                ContainerOperation.Exec(
                    command,
                    process
                )
            )
            Result.success(
                process.await()
            )
        } catch (e: IllegalStateException) {
            Result.failure(e)
        }
    }

    suspend fun shell(): Result<ContainerShell> {
        return exec(listOf("/bin/sh")).map {
            shellFactory.create(it)
        }
    }


    suspend fun start() {
        stateMachine.dispatch(ContainerOperation.Start)
    }

    suspend fun stop() {
        stateMachine.dispatch(ContainerOperation.Stop)
    }

    suspend fun remove() {
        stateMachine.dispatch(ContainerOperation.Remove)
    }

    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted
            initialState: ContainerState
        ): Container
    }
}