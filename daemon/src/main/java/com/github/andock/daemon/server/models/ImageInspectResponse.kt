package com.github.andock.daemon.server.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImageInspectResponse(
    @SerialName("Id")
    val id: String,
    @SerialName("RepoTags")
    val repoTags: List<String>,
    @SerialName("RepoDigests")
    val repoDigests: List<String>,
    @SerialName("Parent")
    val parent: String = "",
    @SerialName("Comment")
    val comment: String = "",
    @SerialName("Created")
    val created: String,
    @SerialName("Container")
    val container: String = "",
    @SerialName("DockerVersion")
    val dockerVersion: String = "",
    @SerialName("Author")
    val author: String = "",
    @SerialName("Config")
    val config: ImageConfigData? = null,
    @SerialName("Architecture")
    val architecture: String,
    @SerialName("Os")
    val os: String,
    @SerialName("Size")
    val size: Long,
    @SerialName("VirtualSize")
    val virtualSize: Long,
    @SerialName("RootFS")
    val rootFS: RootFS
)