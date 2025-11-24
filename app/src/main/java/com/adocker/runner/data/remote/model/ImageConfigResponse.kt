package com.adocker.runner.data.remote.model

import com.adocker.runner.data.local.model.ImageConfig
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