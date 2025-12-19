package com.github.adocker.daemon.mirrors

sealed interface MirrorState {
    val id: String

    data class Unhealthy(
        override val id: String,
    ) : MirrorState

    data class Checking(
        override val id: String,
        val failures: Int
    ) : MirrorState

    data class Healthy(
        override val id: String,
        val latencyMs: Long,
        val failures: Int
    ) : MirrorState, Comparable<Healthy> {
        override fun compareTo(other: Healthy): Int {
            if (this == other) {
                return 0
            } else {
                val c = latencyMs.compareTo(other.latencyMs)
                return if (c == 0) {
                    failures.compareTo(other.failures)
                } else {
                    c
                }
            }
        }
    }

    data class Deleting(
        override val id: String,
    ) : MirrorState

    data class Deleted(
        override val id: String,
    ) : MirrorState
}