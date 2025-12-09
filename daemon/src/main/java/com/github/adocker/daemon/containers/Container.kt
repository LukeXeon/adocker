package com.github.adocker.daemon.containers

import com.github.adocker.daemon.database.model.ContainerEntity
import com.github.adocker.daemon.registry.model.ContainerConfig
import java.io.File

/**
 * Represents a container with its current state and runtime information.
 *
 * This is the unified container model exposed to the UI layer, combining:
 * - Static configuration from [ContainerEntity]
 * - Runtime state from [ContainerState]
 * - Process information
 */
data class Container(
    val id: String,
    val name: String,
    val imageId: String,
    val imageName: String,
    val createdAt: Long,
    val config: ContainerConfig,
    val state: ContainerState,
    val lastRunAt: Long?,
    val stdout: File?,
    val stderr: File?,
    val exitCode: Int?,
)
