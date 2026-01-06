package com.github.andock.daemon.containers

data class ContainerLogKey(
    val containerId: String,
    val currentPage: Int,
    val pageSize: Int,
)
