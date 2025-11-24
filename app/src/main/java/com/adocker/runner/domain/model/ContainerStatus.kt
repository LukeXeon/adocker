package com.adocker.runner.domain.model

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