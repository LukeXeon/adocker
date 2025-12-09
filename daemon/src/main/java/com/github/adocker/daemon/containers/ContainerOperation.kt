package com.github.adocker.daemon.containers

sealed class ContainerOperation {
    class Load(val containerId: String) : ContainerOperation()

    class Start : ContainerOperation()

    class Stop : ContainerOperation()

    class Remove : ContainerOperation()
    class Exec(
        val command: List<String>,
        val callback: (Result<Process>) -> Unit
    ) : ContainerOperation()
}