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
    val created: Long = System.currentTimeMillis(),
    val config: ContainerConfig = ContainerConfig(),
    /**
     * Whether this container has ever been started.
     * Used to distinguish CREATED (never run) from EXITED (ran and stopped).
     */
    val hasRun: Boolean = false,
)