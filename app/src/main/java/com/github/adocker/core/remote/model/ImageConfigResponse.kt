package com.github.adocker.core.remote.model

import com.github.adocker.core.database.model.ImageConfig
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