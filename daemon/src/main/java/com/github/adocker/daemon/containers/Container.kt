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
    stateMachineFactory: ContainerStateMachine.Factory,
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

    private class AbortFlowException(val payload: Any?) : CancellationException()

    private suspend inline fun <reified T : ContainerState, reified R> whenState(
        crossinline block: suspend () -> R
    ): R {
        try {
            stateMachine.state.map {
                it is T
            }.distinctUntilChanged().collectLatest {
                if (it) {
                    throw AbortFlowException(block())
                } else {
                    throw IllegalStateException("container is not ${T::class}")
                }
            }
        } catch (e: AbortFlowException) {
            return e.payload as R
        }
        throw AssertionError()
    }

    suspend fun exec(command: List<String>): Result<ContainerProcess> {
        return try {
            Result.success(
                whenState<ContainerState.Running, ContainerProcess> {
                    val process = CompletableDeferred<ContainerProcess>()
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