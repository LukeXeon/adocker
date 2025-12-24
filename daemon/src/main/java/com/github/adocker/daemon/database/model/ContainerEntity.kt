package com.github.adocker.daemon.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.github.adocker.daemon.client.model.ContainerConfig
import java.util.UUID

@Entity(
    tableName = "containers",
    indices = [Index("id")],
    foreignKeys = [
        // 给imageId加外键：关联image表的id
        ForeignKey(
            entity = ImageEntity::class,
            parentColumns = ["id"],       // 父表（image）的主键
            childColumns = ["imageId"],  // 子表（image_layer_ref）的关联字段
            onDelete = ForeignKey.SET_DEFAULT, // 核心：删除Image时，自动删除该Image的所有引用关系
            onUpdate = ForeignKey.CASCADE, // Image的id更新时，同步更新此处的image_id
        ),
    ]
)
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