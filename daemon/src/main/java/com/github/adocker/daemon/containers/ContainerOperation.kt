package com.github.adocker.daemon.containers

sealed class ContainerOperation {

    object Start : ContainerOperation()

    object Stop : ContainerOperation()

    object Remove : ContainerOperation()
    data class Exec(
        val command: List<String>,
    ) : ContainerOperation()
}