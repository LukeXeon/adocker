package com.github.adocker.core.container

import kotlinx.serialization.Serializable

/**
 * Container runtime status
 */
@Serializable
enum class ContainerStatus {
    CREATED,
    RUNNING,
    PAUSED,
    STOPPED,
    EXITED
}