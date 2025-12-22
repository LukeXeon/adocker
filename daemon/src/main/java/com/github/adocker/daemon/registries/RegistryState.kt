package com.github.adocker.daemon.registries

sealed interface RegistryState {
    val id: String

    data class Unhealthy(
        override val id: String,
    ) : RegistryState

    data class Checking(
        override val id: String,
        val failures: Int
    ) : RegistryState

    data class Healthy(
        override val id: String,
        val latencyMs: Long,
        val failures: Int
    ) : RegistryState

    data class Deleting(
        override val id: String,
    ) : RegistryState

    data class Deleted(
        override val id: String,
    ) : RegistryState
}