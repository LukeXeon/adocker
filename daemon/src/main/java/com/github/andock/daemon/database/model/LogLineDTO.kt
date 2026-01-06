package com.github.andock.daemon.database.model

data class LogLineDTO(
    val id: Long,
    val timestamp: Long,
    val isError: Boolean,
    val message: String
)