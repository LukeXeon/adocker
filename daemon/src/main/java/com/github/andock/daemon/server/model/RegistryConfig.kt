package com.github.andock.daemon.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegistryConfig(
    @SerialName("AllowNondistributableArtifactsCIDRs")
    val allowNondistributableArtifactsCIDRs: List<String>?,
    @SerialName("AllowNondistributableArtifactsHostnames")
    val allowNondistributableArtifactsHostnames: List<String>?,
    @SerialName("InsecureRegistryCIDRs")
    val insecureRegistryCIDRs: List<String>,
    @SerialName("IndexConfigs")
    val indexConfigs: Map<String, IndexConfig>,
    @SerialName("Mirrors")
    val mirrors: List<String>
)