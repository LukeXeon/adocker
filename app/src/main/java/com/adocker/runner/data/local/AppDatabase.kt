package com.adocker.runner.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.adocker.runner.data.local.dao.ContainerDao
import com.adocker.runner.data.local.dao.ImageDao
import com.adocker.runner.data.local.dao.LayerDao
import com.adocker.runner.data.local.dao.MirrorDao
import com.adocker.runner.data.local.entity.ContainerEntity
import com.adocker.runner.data.local.entity.Converters
import com.adocker.runner.data.local.entity.ImageEntity
import com.adocker.runner.data.local.entity.LayerEntity
import com.adocker.runner.data.local.entity.MirrorEntity

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
