package com.adocker.runner.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Platform(
    val architecture: String,
    val os: String,
    val variant: String? = null,
    @SerialName("os.version") val osVersion: String? = null
)