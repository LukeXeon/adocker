package com.github.andock.daemon.containers

import com.github.andock.daemon.database.dao.ContainerDao
import com.github.andock.daemon.os.JobProcess
import com.github.andock.daemon.utils.execute
import com.github.andock.daemon.utils.inState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Singleton

class Container @AssistedInject constructor(
    @Assisted
    initialState: ContainerState,
    stateMachineFactory: ContainerStateMachine.Factory,
    parent: CoroutineScope,
    private val containerDao: ContainerDao,
) {
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

    val metadata
        get() = containerDao.getContainerFlowById(id).stateIn(
            scope,
            SharingStarted.Eagerly,
            null
        )

    init {
        scope.launch {
            stateMachine.state.collect {
                if (it is ContainerState.Removed) {
                    scope.cancel()
                }
            }
        }
    }

    suspend fun exec(command: List<String>): Result<JobProcess> {
        return try {
            Result.success(
                stateMachine.state.inState(
                    ContainerState.Running::class
                ).execute {
                    val process = CompletableDeferred<JobProcess>(
                        currentCoroutineContext()[Job]
                    )
                    stateMachine.dispatch(
                        ContainerOperation.Exec(
                            command,
                            process
                        )
                    )
                    process.await()
                }
            )
        } catch (e: IllegalStateException) {
            Result.failure(e)
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