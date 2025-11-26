package com.github.adocker.daemon.images

/**
 * Image pull progress
 */
data class PullProgress(
    val layerDigest: String,
    val downloaded: Long,
    val total: Long,
    val status: PullStatus
)