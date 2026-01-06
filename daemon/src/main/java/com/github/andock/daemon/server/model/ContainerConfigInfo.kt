package com.github.andock.daemon.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContainerConfigInfo(
    @SerialName("Hostname")
    val hostname: String,
    @SerialName("User")
    val user: String,
    @SerialName("Env")
    val env: List<String>,
    @SerialName("Cmd")
    val cmd: List<String>,
    @SerialName("Image")
    val image: String,
    @SerialName("WorkingDir")
    val workingDir: String,
    @SerialName("Entrypoint")
    val entrypoint: List<String>? = null
)