package com.github.andock.daemon.client.model

import kotlinx.serialization.Serializable

/**
 * Manifest Descriptor
 *
 * References a platform-specific image manifest within a manifest list.
 * Contains content addressing information for validation and retrieval.
 *
 * @property mediaType MIME type of the referenced manifest (e.g., "application/vnd.docker.distribution.manifest.v2+json")
 * @property digest Content hash in format "algorithm:hex" (e.g., "sha256:abc123...")
 * @property size Size of the manifest in bytes, used for validation
 * @property platform Optional platform specification (architecture, OS, variant) for multi-arch images
 */
@Serializable
data class ManifestDescriptor(
    val mediaType: String,
    val digest: String,
    val size: Long,
    val platform: Platform? = null
)