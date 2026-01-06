package com.github.andock.daemon.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImageDiskUsage(
    @SerialName("Id")
    val id: String,
    @SerialName("ParentId")
    val parentId: String,
    @SerialName("RepoTags")
    val repoTags: List<String>,
    @SerialName("RepoDigests")
    val repoDigests: List<String>,
    @SerialName("Created")
    val created: Long,
    @SerialName("Size")
    val size: Long,
    @SerialName("SharedSize")
    val sharedSize: Long,
    @SerialName("VirtualSize")
    val virtualSize: Long,
    @SerialName("Labels")
    val labels: Map<String, String>?,
    @SerialName("Containers")
    val containers: Int
)