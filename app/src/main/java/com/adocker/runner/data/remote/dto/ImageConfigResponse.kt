package com.adocker.runner.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ImageConfigResponse(
    val architecture: String? = null,
    val os: String? = null,
    val config: ContainerConfigDto? = null,
    val rootfs: RootfsDto? = null,
    val history: List<HistoryDto>? = null,
    val created: String? = null
)