package com.github.andock.daemon.images

import androidx.collection.LruCache
import androidx.collection.lruCache
import com.github.andock.daemon.database.dao.RegistryDao
import com.github.andock.daemon.database.model.RegistryEntity
import com.github.andock.daemon.utils.SuspendLazy
import com.github.andock.daemon.utils.suspendLazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ImagesModule {

    @Provides
    @Singleton
    @Named("images")
    fun initializer(
        @Named("app")
        app: SuspendLazy<Unit>,
        imageManager: ImageManager,
    ) = suspendLazy {
        app.getValue()
        imageManager.deleteUnreferencedLayers()
    }

    @Provides
    @IntoMap
    @StringKey("images")
    fun initializerToMap(
        @Named("images") task: SuspendLazy<Unit>
    ): SuspendLazy<*> = task

    @Provides
    @Singleton
    fun imageRepositories(
        scope: CoroutineScope,
        registryDao: RegistryDao,
        builtinServers: List<RegistryEntity>,
        factory: ImageRepository.Factory
    ): LruCache<String, ImageRepository> {
        val cache = lruCache(builtinServers.size, create = factory)
        scope.launch {
            registryDao.getRegistryCountFlow().collect {
                cache.resize(maxOf(1, builtinServers.size, it))
            }
        }
        return cache
    }
}