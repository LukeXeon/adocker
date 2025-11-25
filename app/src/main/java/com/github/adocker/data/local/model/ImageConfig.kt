package com.github.adocker.data.local.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Image configuration from manifest
 */
@Serializable
data class ImageConfig(
    @SerialName("Cmd") val cmd: List<String>? = null,
    @SerialName("Entrypoint") val entrypoint: List<String>? = null,
    @SerialName("Env") val env: List<String>? = null,
    @SerialName("WorkingDir") val workingDir: String? = null,
    @SerialName("User") val user: String? = null,
    @SerialName("ExposedPorts") val exposedPorts: Map<String, @Serializable(with = EmptyObjectSerializer::class) Unit>? = null,
    @SerialName("Volumes") val volumes: Map<String, @Serializable(with = EmptyObjectSerializer::class) Unit>? = null,
    @SerialName("Labels") val labels: Map<String, String>? = null
)