package com.github.andock.daemon.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContainerCreateRequest(
    @SerialName("Image")
    val image: String,
    @SerialName("Cmd")
    val cmd: List<String>? = null,
    @SerialName("Entrypoint")
    val entrypoint: List<String>? = null,
    @SerialName("Env")
    val env: List<String>? = null,
    @SerialName("WorkingDir")
    val workingDir: String? = null,
    @SerialName("User")
    val user: String? = null,
    @SerialName("Hostname")
    val hostname: String? = null,
    @SerialName("HostConfig")
    val hostConfig: HostConfig? = null
)