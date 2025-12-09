package com.github.adocker.daemon.containers

import com.github.adocker.daemon.registry.model.ContainerConfig
import kotlinx.coroutines.CompletableDeferred

sealed class ContainerOperation {

    class Create(
        val imageId: String,
        val name: String?,
        val config: ContainerConfig
    ) : ContainerOperation()

    class Load(
        val containerId: String
    ) : ContainerOperation()

    class Start : ContainerOperation()

    class Stop : ContainerOperation()

    class Remove : ContainerOperation()

    class Exec(
        val command: List<String>
    ) : ContainerOperation() {
        val process = CompletableDeferred<Process>()
    }
}