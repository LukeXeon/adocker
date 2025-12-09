package com.github.adocker.daemon.containers

import com.freeletics.flowredux2.FlowReduxStateMachine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.StateFlow

class Container(
    val containerId: String,
    private val stateMachine: FlowReduxStateMachine<StateFlow<ContainerState>, ContainerOperation>,
) {
    suspend fun exec(command: List<String>): Process {
        val deferred = CompletableDeferred<Process>()
        stateMachine.dispatch(ContainerOperation.Exec(command, deferred))
        return deferred.await()
    }

    val state
        get() = stateMachine.state

    suspend fun start() {
        stateMachine.dispatch(ContainerOperation.Start)
    }

    suspend fun stop() {
        stateMachine.dispatch(ContainerOperation.Stop)
    }

    suspend fun remove() {
        stateMachine.dispatch(ContainerOperation.Remove)
    }
}