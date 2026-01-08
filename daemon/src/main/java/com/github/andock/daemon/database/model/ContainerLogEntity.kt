package com.github.andock.daemon.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    "log_lines",
    indices = [Index("timestamp"), Index("containerId")],
    foreignKeys = [
        // 给containerId加外键：关联containers表的id
        ForeignKey(
            entity = ContainerEntity::class,
            parentColumns = ["id"],       // 父表（Container）的主键
            childColumns = ["containerId"],  // 子表（log_lines）的关联字段
            onDelete = ForeignKey.CASCADE, // 核心：删除Container时，自动删除该LogLine的所有引用关系
            onUpdate = ForeignKey.CASCADE, // Container的id更新时，同步更新此处的containerId
        ),
    ]
)
data class ContainerLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val timestamp: Long,
    val containerId: String,
    val isError: Boolean,
    val message: String
)