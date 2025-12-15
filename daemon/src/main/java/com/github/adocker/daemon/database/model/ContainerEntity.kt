package com.github.adocker.daemon.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.github.adocker.daemon.registry.model.ContainerConfig
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(tableName = "containers")
@TypeConverters(Converters::class)
data class ContainerEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val imageId: String,
    val imageName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val config: ContainerConfig = ContainerConfig(),
    val lastRunAt: Long? = null,
)