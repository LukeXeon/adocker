package com.github.adocker.data.repository.model

import com.github.adocker.data.repository.model.PullStatus

/**
 * Image pull progress
 */
data class PullProgress(
    val layerDigest: String,
    val downloaded: Long,
    val total: Long,
    val status: PullStatus
)