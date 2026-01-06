package com.github.andock.daemon.images.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Platform
 *
 * Specifies the target platform for a container image in a manifest list.
 * Enables multi-architecture support by identifying CPU, OS, and variants.
 *
 * @property architecture CPU architecture (e.g., "amd64", "arm64", "arm") following Go GOARCH values
 * @property os Operating system (e.g., "linux", "windows", "darwin") following Go GOOS values
 * @property variant Optional CPU variant (e.g., "v7", "v8" for ARM)
 * @property osVersion Optional OS version for platform-specific images (e.g., Windows build numbers)
 */
@Serializable
data class Platform(
    val architecture: String,
    val os: String,
    val variant: String? = null,
    @SerialName("os.version") val osVersion: String? = null
)