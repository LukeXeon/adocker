package com.github.adocker.core.di

import android.content.Context
import androidx.room.Room
import com.github.adocker.core.config.AppConfig
import com.github.adocker.core.database.AppDatabase
import com.github.adocker.core.database.dao.ContainerDao
import com.github.adocker.core.database.dao.ImageDao
import com.github.adocker.core.database.dao.LayerDao
import com.github.adocker.core.database.dao.MirrorDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
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
    fun mirrorDao(database: AppDatabase): MirrorDao {
        return database.mirrorDao()
    }

    @Provides
    @Singleton
    fun provideLayerDao(database: AppDatabase): LayerDao {
        return database.layerDao()
    }

    @Provides
    @Singleton
    fun json(): Json {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            prettyPrint = true
        }
    }

    @Provides
    @Singleton
    fun httpClient(
        json: Json,
        appConfig: AppConfig
    ): HttpClient {
        return HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(json)
            }
            install(Logging) {
                level = LogLevel.HEADERS
            }
            install(HttpTimeout) {
                requestTimeoutMillis = AppConfig.NETWORK_TIMEOUT
                connectTimeoutMillis = AppConfig.NETWORK_TIMEOUT
                socketTimeoutMillis = AppConfig.DOWNLOAD_TIMEOUT
            }
            defaultRequest {
                header(
                    HttpHeaders.UserAgent,
                    "${requireNotNull(appConfig.packageInfo.applicationInfo).name}/${appConfig.packageInfo.versionName}"
                )
            }
        }
    }


}
