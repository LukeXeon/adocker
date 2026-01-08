package com.github.andock.daemon.database.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    "in_memory_log_lines",
    indices = [Index("timestamp"), Index("sessionId")],
)
data class InMemoryLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val timestamp: Long,
    val sessionId: String,
    val isError: Boolean,
    val message: String
)