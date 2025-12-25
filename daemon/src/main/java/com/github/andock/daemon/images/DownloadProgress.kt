package com.github.andock.daemon.images

sealed interface DownloadProgress {
    val total: Long

    data class Downloading(
        override val total: Long,
        val downloaded: Long,
    ) : DownloadProgress

    data class Downloaded(
        override val total: Long,
    ) : DownloadProgress
}