package com.github.andock.daemon.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VolumeUsageData(
    @SerialName("Size")
    val size: Long,
    @SerialName("RefCount")
    val refCount: Int
)