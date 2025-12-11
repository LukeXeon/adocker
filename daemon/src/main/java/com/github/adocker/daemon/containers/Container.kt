package com.github.adocker.daemon.containers

import com.freeletics.flowredux2.FlowReduxStateMachine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.StateFlow

class Container(
    val containerId: String,
    private val stateMachine: FlowReduxStateMachine<StateFlow<ContainerState>, ContainerOperation>,
) {
    val state
        get() = stateMachine.state

    suspend fun exec(command: List<String>): Process {
        val exec = ContainerOperation.Exec(command)
        stateMachine.dispatch(exec)
        return exec.deferred.await()
    }

    fun start() {
        stateMachine.dispatchAction(ContainerOperation.Start)
    }

    fun stop() {
        stateMachine.dispatchAction(ContainerOperation.Stop)
    }

    fun remove() {
        stateMachine.dispatchAction(ContainerOperation.Remove)
    }
}