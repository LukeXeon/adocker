package com.github.andock.daemon.images

data class DownloadProgress(
    val downloaded: Long,
    val total: Long,
)