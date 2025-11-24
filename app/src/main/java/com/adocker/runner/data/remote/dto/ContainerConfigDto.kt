package com.adocker.runner.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContainerConfigDto(
    @SerialName("Cmd") val cmd: List<String>? = null,
    @SerialName("Entrypoint") val entrypoint: List<String>? = null,
    @SerialName("Env") val env: List<String>? = null,
    @SerialName("WorkingDir") val workingDir: String? = null,
    @SerialName("User") val user: String? = null,
    @SerialName("ExposedPorts") val exposedPorts: Map<String, EmptyObject>? = null,
    @SerialName("Volumes") val volumes: Map<String, EmptyObject>? = null,
    @SerialName("Labels") val labels: Map<String, String>? = null
)