package com.github.adocker.daemon.containers

import com.freeletics.flowredux2.FlowReduxStateMachine
import kotlinx.coroutines.flow.StateFlow

class Container(
    val containerId: String,
    private val stateMachine: FlowReduxStateMachine<StateFlow<ContainerState>, ContainerOperation>,
)