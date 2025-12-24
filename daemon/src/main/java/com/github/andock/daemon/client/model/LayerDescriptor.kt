package com.github.andock.daemon.client.model

import kotlinx.serialization.Serializable

/**
 * Layer Descriptor
 *
 * References a filesystem layer blob (typically a gzip-compressed tar archive).
 * Layers are applied in order to construct the container's root filesystem.
 *
 * @property mediaType MIME type of the layer blob (typically "application/vnd.docker.image.rootfs.diff.tar.gzip")
 * @property digest Content hash of the compressed layer in format "algorithm:hex" (e.g., "sha256:abc123...")
 * @property size Size of the compressed layer in bytes, used for validation and progress tracking
 * @property urls Optional list of URLs where the layer can be downloaded (for foreign layers)
 */
@Serializable
data class LayerDescriptor(
    val mediaType: String,
    val digest: String,
    val size: Long,
    val urls: List<String>? = null
)