package com.github.andock.daemon.containers.creator

import com.github.andock.daemon.containers.Container
import com.github.andock.daemon.images.models.ContainerConfig

interface ContainerCreateState {
    val id: String

    data class Creating(
        override val id: String,
        val imageId: String,
        val name: String?,
        val config: ContainerConfig
    ) : ContainerCreateState

    data class Error(
        override val id: String,
        val throwable: Throwable
    ) : ContainerCreateState

    data class Done(
        val container: Container
    ) : ContainerCreateState {
        override val id
            get() = container.id
    }
}