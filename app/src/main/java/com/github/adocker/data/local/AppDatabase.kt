package com.github.adocker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.github.adocker.data.local.dao.ContainerDao
import com.github.adocker.data.local.dao.ImageDao
import com.github.adocker.data.local.dao.LayerDao
import com.github.adocker.data.local.dao.MirrorDao
import com.github.adocker.data.local.model.ContainerEntity
import com.github.adocker.data.local.model.Converters
import com.github.adocker.data.local.model.ImageEntity
import com.github.adocker.data.local.model.LayerEntity
import com.github.adocker.data.local.model.MirrorEntity

@Database(
    entities = [
        ImageEntity::class,
        ContainerEntity::class,
        LayerEntity::class,
        MirrorEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun imageDao(): ImageDao
    abstract fun containerDao(): ContainerDao
    abstract fun layerDao(): LayerDao
    abstract fun mirrorDao(): MirrorDao
}
