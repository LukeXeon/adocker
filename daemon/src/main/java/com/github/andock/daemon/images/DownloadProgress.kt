package com.github.andock.daemon.images

sealed interface DownloadProgress {
    val name: String
    val total: Long

    data class Downloading(
        override val name: String,
        override val total: Long,
        val downloaded: Long,
    ) : DownloadProgress

    data class Downloaded(
        override val name: String,
        override val total: Long,
    ) : DownloadProgress
}