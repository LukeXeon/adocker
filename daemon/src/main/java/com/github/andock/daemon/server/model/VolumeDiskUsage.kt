package com.github.andock.daemon.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VolumeDiskUsage(
    @SerialName("Name")
    val name: String,
    @SerialName("Driver")
    val driver: String,
    @SerialName("Mountpoint")
    val mountpoint: String,
    @SerialName("CreatedAt")
    val createdAt: String,
    @SerialName("Status")
    val status: Map<String, String>?,
    @SerialName("Labels")
    val labels: Map<String, String>?,
    @SerialName("Scope")
    val scope: String,
    @SerialName("Options")
    val options: Map<String, String>?,
    @SerialName("UsageData")
    val usageData: VolumeUsageData?
)