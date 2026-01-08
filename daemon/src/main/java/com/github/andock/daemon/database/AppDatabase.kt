package com.github.andock.daemon.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.github.andock.daemon.database.dao.AuthTokenDao
import com.github.andock.daemon.database.dao.ContainerDao
import com.github.andock.daemon.database.dao.ContainerLogDao
import com.github.andock.daemon.database.dao.ImageDao
import com.github.andock.daemon.database.dao.LayerDao
import com.github.andock.daemon.database.dao.RegistryDao
import com.github.andock.daemon.database.dao.SearchRecordDao
import com.github.andock.daemon.database.model.AuthTokenEntity
import com.github.andock.daemon.database.model.ContainerEntity
import com.github.andock.daemon.database.model.ContainerLogEntity
import com.github.andock.daemon.database.model.ImageEntity
import com.github.andock.daemon.database.model.LayerEntity
import com.github.andock.daemon.database.model.LayerReferenceEntity
import com.github.andock.daemon.database.model.RegistryEntity
import com.github.andock.daemon.database.model.SearchRecordEntity

@Database(
    entities = [
        ImageEntity::class,
        ContainerEntity::class,
        LayerEntity::class,
        LayerReferenceEntity::class,
        RegistryEntity::class,
        AuthTokenEntity::class,
        SearchRecordEntity::class,
        ContainerLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val imageDao: ImageDao
    abstract val containerDao: ContainerDao
    abstract val layerDao: LayerDao
    abstract val registryDao: RegistryDao
    abstract val authTokenDao: AuthTokenDao
    abstract val searchRecordDao: SearchRecordDao
    abstract val logLineDao: ContainerLogDao
}
