package com.github.andock.daemon.database.model

import java.util.UUID

data class ContainerDTO(
    val id: String = UUID.randomUUID().toString(),
    val lastRunAt: Long? = null,
)
