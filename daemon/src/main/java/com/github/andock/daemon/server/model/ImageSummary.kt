package com.github.andock.daemon.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImageSummary(
    @SerialName("Id")
    val id: String,
    @SerialName("ParentId")
    val parentId: String = "",
    @SerialName("RepoTags")
    val repoTags: List<String>,
    @SerialName("RepoDigests")
    val repoDigests: List<String>,
    @SerialName("Created")
    val created: Long,
    @SerialName("Size")
    val size: Long,
    @SerialName("VirtualSize")
    val virtualSize: Long,
    @SerialName("SharedSize")
    val sharedSize: Long = 0,
    @SerialName("Labels")
    val labels: Map<String, String>? = null,
    @SerialName("Containers")
    val containers: Int = 0
)