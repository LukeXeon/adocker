package com.github.adocker.daemon.client.model

import kotlinx.serialization.Serializable

/**
 * Config Descriptor
 *
 * References the image configuration blob containing container runtime settings,
 * architecture, OS, and layer diff IDs.
 *
 * @property mediaType MIME type of the config blob (typically "application/vnd.docker.container.image.v1+json")
 * @property digest Content hash of the config blob in format "algorithm:hex" (e.g., "sha256:abc123...")
 * @property size Size of the config blob in bytes, used for validation before download
 */
@Serializable
data class ConfigDescriptor(
    val mediaType: String,
    val digest: String,
    val size: Long
)