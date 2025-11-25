package com.github.adocker.core.database.model

import kotlinx.serialization.Serializable

/**
 * Container configuration
 */
@Serializable
data class ContainerConfig(
    val cmd: List<String> = listOf("/bin/sh"),
    val entrypoint: List<String>? = null,
    val env: Map<String, String> = emptyMap(),
    val workingDir: String = "/",
    val user: String = "root",
    val hostname: String = "localhost",
    val binds: List<VolumeBinding> = emptyList(),
    val portBindings: Map<String, String> = emptyMap(),
    val networkEnabled: Boolean = false
)