package com.github.adocker.daemon.containers

import com.github.adocker.daemon.database.dao.ContainerDao
import com.github.adocker.daemon.database.model.ContainerEntity
import com.github.adocker.daemon.os.JobProcess
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

    val containerId
        get() = state.value.containerId

    val state
        get() = stateMachine.state

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
                stateMachine.state.inState<ContainerState.Running>().execute {
                    val process = CompletableDeferred<JobProcess>()
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

    suspend fun getInfo(): Result<ContainerEntity> {
        val entity = containerDao.getContainerById(containerId)
        return if (entity != null) {
            Result.success(entity)
        } else {
            Result.failure(NoSuchElementException("Container not found: $containerId"))
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