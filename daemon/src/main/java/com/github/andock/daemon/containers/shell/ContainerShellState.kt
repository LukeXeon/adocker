package com.github.andock.daemon.containers.shell

sealed interface ContainerShellState {
    val process: Process

    data class Running(
        override val process: Process
    ) : ContainerShellState

    data class Exited(
        override val process: Process
    ) : ContainerShellState
}