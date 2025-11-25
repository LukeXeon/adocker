package com.github.adocker.core.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Platform(
    val architecture: String,
    val os: String,
    val variant: String? = null,
    @SerialName("os.version") val osVersion: String? = null
)