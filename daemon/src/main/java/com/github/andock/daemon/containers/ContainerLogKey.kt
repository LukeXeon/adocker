package com.github.andock.daemon.containers

data class ContainerLogKey(
    val containerId: String,
    val currentPage: Long,
    val pageSize: Long,
)
