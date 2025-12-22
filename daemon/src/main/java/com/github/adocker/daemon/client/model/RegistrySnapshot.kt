package com.github.adocker.daemon.client.model

import com.github.adocker.daemon.database.model.RegistryEntity
import com.github.adocker.daemon.registries.RegistryState

data class RegistrySnapshot(
    val metadata: RegistryEntity,
    val state: RegistryState.Healthy
) : Comparable<RegistrySnapshot> {
    override fun compareTo(other: RegistrySnapshot): Int {
        var compare = state.latencyMs.compareTo(other.state.latencyMs)
        if (compare != 0) {
            return compare
        }
        compare = state.failures.compareTo(other.state.failures)
        if (compare != 0) {
            return compare
        }
        compare = other.metadata.priority.compareTo(metadata.priority)
        if (compare != 0) {
            return compare
        }
        compare = metadata.type.compareTo(other.metadata.type)
        if (compare != 0) {
            return compare
        }
        return 0
    }
}