package com.github.andock.daemon.containers.shell

sealed interface ContainerShellState {
    val id: String

    data class Running(
        override val id: String,
        val process: Process
    ) : ContainerShellState

    data class Exited(
        override val id: String,
        val exitCode: Int
    ) : ContainerShellState
}