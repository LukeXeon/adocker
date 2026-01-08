package com.github.andock.daemon.database.model

data class ContainerLogDTO(
    val id: Long,
    val timestamp: Long,
    val message: String
)