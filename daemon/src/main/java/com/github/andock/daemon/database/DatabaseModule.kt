package com.github.andock.daemon.database

import android.app.Application
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun appDatabase(application: Application) = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "shared_database"
    ).build()


    @Provides
    @Singleton
    fun imageDao(database: AppDatabase) = database.imageDao()

    @Provides
    @Singleton
    fun containerDao(database: AppDatabase) = database.containerDao()

    @Provides
    @Singleton
    fun mirrorDao(database: AppDatabase) = database.registryDao()

    @Provides
    @Singleton
    fun layerDao(database: AppDatabase) = database.layerDao()

    @Provides
    @Singleton
    fun authTokenDao(database: AppDatabase) = database.authTokenDao()

    @Provides
    @Singleton
    fun searchRecordDao(database: AppDatabase) = database.searchRecordDao()

    @Provides
    @Singleton
    fun logLineDao(database: AppDatabase) = database.logLineDao()
}