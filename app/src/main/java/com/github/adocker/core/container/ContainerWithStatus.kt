package com.github.adocker.core.container

import com.github.adocker.core.database.model.ContainerEntity
import com.github.adocker.core.database.model.ContainerStatus

/**
 * Simple wrapper that combines a container entity with its runtime status
 *
 * This separates data (from database) from runtime state (from executor).
 * Use properties from entity directly, e.g., container.entity.name
 */
data class ContainerWithStatus(
    val entity: ContainerEntity,
    val status: ContainerStatus
) {
    // Convenience accessors to avoid .entity everywhere
    val id: String get() = entity.id
    val name: String get() = entity.name
    val imageId: String get() = entity.imageId
    val imageName: String get() = entity.imageName
    val created: Long get() = entity.created
    val config get() = entity.config
}
