package com.github.adocker.daemon.containers

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Singleton

class Container @AssistedInject constructor(
    @Assisted
    initialState: ContainerState,
    stateMachineFactory: ContainerStateMachineSpec.Factory,
    parentScope: CoroutineScope,
) {
    init {
        require(initialState is ContainerState.Created || initialState is ContainerState.Exited)
    }

    private val scope = CoroutineScope(
        SupervisorJob(parentScope.coroutineContext[Job]) + Dispatchers.IO
    )
    private val stateMachine = stateMachineFactory.create(initialState).launchIn(scope)

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

    private class JumpOutException(val process: ContainerProcess) : CancellationException()

    suspend fun exec(command: List<String>): Result<ContainerProcess> {
        try {
            stateMachine.state.map {
                it is ContainerState.Running
            }.distinctUntilChanged().collectLatest {
                if (it) {
                    val process = CompletableDeferred<ContainerProcess>()
                    stateMachine.dispatch(
                        ContainerOperation.Exec(
                            command,
                            process
                        )
                    )
                    throw JumpOutException(process.await())
                } else {
                    throw IllegalStateException("Cannot exec: container is not running")
                }
            }
        } catch (e: JumpOutException) {
            return Result.success(e.process)
        } catch (e: IllegalStateException) {
            return Result.failure(e)
        }
        throw AssertionError()
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