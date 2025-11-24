package com.adocker.runner.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Container definition
 */
@Serializable
data class Container(
    val id: String = UUID.randomUUID().toString().take(12),
    val name: String,
    val imageId: String,
    val imageName: String,
    val created: Long = System.currentTimeMillis(),
    val status: ContainerStatus = ContainerStatus.CREATED,
    val config: ContainerConfig = ContainerConfig(),
    val pid: Int? = null
)