package com.github.andock.daemon.database.model

data class ContainerLogDTO(
    val id: Long,
    val timestamp: Long,
    val isError: Boolean,
    val message: String
)