package com.github.andock.daemon.client.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Root Filesystem
 *
 * Describes the root filesystem layers and their content addresses.
 * DiffIDs are hashes of the uncompressed layer tar archives.
 *
 * @property type Must be "layers" per the OCI specification
 * @property diffIds Ordered array of layer DiffIDs (sha256 hashes of uncompressed tar files) in application order
 */
@Serializable
data class Rootfs(
    val type: String,
    @SerialName("diff_ids") val diffIds: List<String>
)