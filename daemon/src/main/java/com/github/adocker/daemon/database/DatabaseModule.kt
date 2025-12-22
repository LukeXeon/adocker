package com.github.adocker.daemon.database

import android.content.Context
import androidx.room.Room
import com.github.adocker.daemon.database.dao.ContainerDao
import com.github.adocker.daemon.database.dao.ImageDao
import com.github.adocker.daemon.database.dao.LayerDao
import com.github.adocker.daemon.database.dao.RegistryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "shared_database"
        ).build()
    }


    @Provides
    @Singleton
    fun provideImageDao(database: AppDatabase): ImageDao {
        return database.imageDao()
    }

    @Provides
    @Singleton
    fun provideContainerDao(database: AppDatabase): ContainerDao {
        return database.containerDao()
    }

    @Provides
    @Singleton
    fun mirrorDao(database: AppDatabase): RegistryDao {
        return database.registryDao()
    }

    @Provides
    @Singleton
    fun provideLayerDao(database: AppDatabase): LayerDao {
        return database.layerDao()
    }
}