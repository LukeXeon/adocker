package com.github.andock.daemon.client

data class DownloadProgress(
    val downloaded: Long,
    val total: Long,
)