package com.github.andock.daemon.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiskUsage(
    @SerialName("LayersSize")
    val layersSize: Long,
    @SerialName("Images")
    val images: List<ImageDiskUsage>,
    @SerialName("Containers")
    val containers: List<ContainerDiskUsage>,
    @SerialName("Volumes")
    val volumes: List<VolumeDiskUsage>
)

