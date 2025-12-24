package com.github.adocker.daemon.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "layer_references",
    primaryKeys = ["imageId", "layerDigest"],
    indices = [
        Index(value = ["imageId"]),
        Index(value = ["layerDigest"])
    ],
    foreignKeys = [
        // 给imageId加外键：关联image表的id
        ForeignKey(
            entity = ImageEntity::class,
            parentColumns = ["id"],       // 父表（image）的主键
            childColumns = ["imageId"],  // 子表（image_layer_ref）的关联字段
            onDelete = ForeignKey.CASCADE, // 核心：删除Image时，自动删除该Image的所有引用关系
            onUpdate = ForeignKey.CASCADE, // Image的id更新时，同步更新此处的image_id
        ),
        // 给layerDigest加外键：关联layer表的id
        ForeignKey(
            entity = LayerEntity::class,
            parentColumns = ["digest"],
            childColumns = ["layerDigest"],
            onDelete = ForeignKey.RESTRICT, // 删除Layer时，若有引用则禁止删除（避免误删）
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class LayerReference(
    val imageId: String,
    val layerDigest: String,
)