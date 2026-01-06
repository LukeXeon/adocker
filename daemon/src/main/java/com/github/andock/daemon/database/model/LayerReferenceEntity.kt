package com.github.andock.daemon.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "layer_references",
    primaryKeys = ["imageId", "layerId"],
    indices = [
        Index(value = ["imageId"]),
        Index(value = ["layerId"])
    ],
    foreignKeys = [
        // 给imageId加外键：关联image表的id
        ForeignKey(
            entity = ImageEntity::class,
            parentColumns = ["id"],       // 父表（image）的主键
            childColumns = ["imageId"],  // 子表（image_layer_ref）的关联字段
            onDelete = ForeignKey.CASCADE, // 核心：删除Image时，自动删除该Image的所有引用关系
            onUpdate = ForeignKey.CASCADE, // Image的id更新时，同步更新此处的imageId
        ),
        // 给layerDigest加外键：关联layer表的id
        ForeignKey(
            entity = LayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["layerId"],
            onDelete = ForeignKey.RESTRICT, // 删除Layer时，若有引用则禁止删除（避免误删）
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class LayerReferenceEntity(
    val imageId: String,
    val layerId: String,
)