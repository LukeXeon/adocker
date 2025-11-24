package com.adocker.runner.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "registry_mirrors")
data class MirrorEntity(
    @PrimaryKey
    val url: String,
    val name: String,
    val isDefault: Boolean = false,
    val isBuiltIn: Boolean = true,
    val isSelected: Boolean = false
)