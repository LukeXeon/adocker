package com.github.andock.daemon.registries

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

    ) : RegistryState, Comparable<Healthy> {
        override fun compareTo(other: Healthy): Int {
            var compare = latencyMs.compareTo(other.latencyMs)
            if (compare != 0) {
                return compare
            }
            compare = failures.compareTo(other.failures)
            if (compare != 0) {
                return compare
            }
            return 0
        }
    }

    data class Removing(
        override val id: String,
    ) : RegistryState

    data class Removed(
        override val id: String,
    ) : RegistryState
}