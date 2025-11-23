package com.adocker.runner.core.di

import android.content.Context
import com.adocker.runner.core.config.AppConfig
import com.adocker.runner.core.config.RegistrySettingsManager
import timber.log.Timber
import com.adocker.runner.data.local.AppDatabase
import com.adocker.runner.data.local.dao.ContainerDao
import com.adocker.runner.data.local.dao.ImageDao
import com.adocker.runner.data.local.dao.LayerDao
import com.adocker.runner.data.remote.api.DockerRegistryApi
import com.adocker.runner.data.repository.ContainerRepository
import com.adocker.runner.data.repository.ImageRepository
import com.adocker.runner.engine.executor.ContainerExecutor
import com.adocker.runner.engine.proot.PRootEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
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
    fun provideLayerDao(database: AppDatabase): LayerDao {
        return database.layerDao()
    }

    @Provides
    @Singleton
    fun provideRegistrySettingsManager(@ApplicationContext context: Context): RegistrySettingsManager {
        return RegistrySettingsManager(context)
    }

    @Provides
    @Singleton
    fun provideDockerRegistryApi(
        registrySettings: RegistrySettingsManager,
        appConfig: AppConfig
    ): DockerRegistryApi {
        return DockerRegistryApi(registrySettings, appConfig)
    }

    @Provides
    @Singleton
    fun provideImageRepository(
        imageDao: ImageDao,
        layerDao: LayerDao,
        registryApi: DockerRegistryApi,
        appConfig: AppConfig
    ): ImageRepository {
        return ImageRepository(imageDao, layerDao, registryApi, appConfig)
    }

    @Provides
    @Singleton
    fun provideContainerRepository(
        containerDao: ContainerDao,
        imageDao: ImageDao,
        appConfig: AppConfig
    ): ContainerRepository {
        return ContainerRepository(containerDao, imageDao, appConfig)
    }

    @Provides
    @Singleton
    fun providePRootEngine(appConfig: AppConfig): PRootEngine? {
        return runBlocking {
            initializePRoot(appConfig)
        }
    }

    @Provides
    @Singleton
    fun provideContainerExecutor(
        prootEngine: PRootEngine?,
        containerRepository: ContainerRepository
    ): ContainerExecutor? {
        return prootEngine?.let { engine ->
            ContainerExecutor(engine, containerRepository)
        }
    }

    /**
     * Initialize PRoot directly from native library directory.
     *
     * IMPORTANT: On Android 10+ (API 29+), binaries can only be executed from directories
     * with the correct SELinux context. The native library directory (/data/app/<pkg>/lib/<arch>)
     * has 'apk_data_file' context which allows execution. The app data directory
     * (/data/data/<pkg>) has 'app_data_file' context which blocks execution (execute_no_trans).
     *
     * Therefore, PRoot must be executed DIRECTLY from the native library directory.
     * Do NOT copy binaries to app data directory - this will fail on Android 10+.
     */
    private suspend fun initializePRoot(appConfig: AppConfig): PRootEngine? {
        val nativeLibDir = appConfig.nativeLibDir

        if (nativeLibDir == null) {
            Timber.e("Native library directory is null")
            return null
        }

        // Execute PRoot directly from native lib dir (has apk_data_file SELinux context)
        val prootBinary = File(nativeLibDir, "libproot.so")

        if (!prootBinary.exists()) {
            Timber.e("PRoot binary not found at: ${prootBinary.absolutePath}")
            return null
        }

        Timber.d("Initializing PRoot from native lib dir: ${prootBinary.absolutePath}")

        // List files in native lib dir for debugging
        nativeLibDir.listFiles()?.forEach { file ->
            Timber.d("  Native lib: ${file.name} (${file.length()} bytes)")
        }

        val engine = PRootEngine(prootBinary, nativeLibDir, appConfig)
        return if (engine.isAvailable()) {
            Timber.d("PRoot engine initialized successfully")
            engine
        } else {
            Timber.e("PRoot engine not available")
            null
        }
    }
}
