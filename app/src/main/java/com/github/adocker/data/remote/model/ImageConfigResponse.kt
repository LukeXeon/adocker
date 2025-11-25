package com.github.adocker.data.remote.model

import com.github.adocker.data.local.model.ImageConfig
import kotlinx.serialization.Serializable

@Serializable
data class ImageConfigResponse(
    val architecture: String? = null,
    val os: String? = null,
    val config: ImageConfig? = null,
    val rootfs: Rootfs? = null,
    val history: List<History>? = null,
    val created: String? = null
)