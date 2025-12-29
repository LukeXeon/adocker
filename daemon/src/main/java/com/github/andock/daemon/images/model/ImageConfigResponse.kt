package com.github.andock.daemon.images.model

import kotlinx.serialization.Serializable

/**
 * Image Configuration Response
 *
 * The complete image configuration blob referenced by the manifest's config descriptor.
 * Contains platform information, execution parameters, and layer metadata.
 * Conforms to: OCI Image Specification / Docker Image Spec v1.2
 *
 * @property architecture CPU architecture (e.g., "amd64", "arm64") following Go GOARCH values
 * @property os Operating system (e.g., "linux", "windows") following Go GOOS values
 * @property config Container execution configuration (command, environment, exposed ports, etc.)
 * @property rootfs Root filesystem layer information with ordered diff IDs
 * @property history Layer creation history and metadata
 * @property created RFC 3339 formatted timestamp of image creation
 */
@Serializable
data class ImageConfigResponse(
    val architecture: String? = null,
    val os: String? = null,
    val config: ImageConfig? = null,
    val rootfs: Rootfs? = null,
    val history: List<History>? = null,
    val created: String? = null
)