package com.github.andock.daemon.containers

import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.database.dao.ContainerDao
import com.github.andock.daemon.utils.SuspendLazy
import com.github.andock.daemon.utils.suspendLazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ContainersModule {

    @Provides
    @Singleton
    @Named("containers")
    fun initializer(
        @Named("app")
        app: SuspendLazy<Unit>,
        appContext: AppContext,
        containerDao: ContainerDao,
    ) = suspendLazy {
        app.getValue()
        withContext(Dispatchers.IO) {
            val containers = containerDao.getAllContainerIds().map {
                File(appContext.containersDir, it)
            }.toSet()
            appContext.containersDir.listFiles {
                !containers.contains(it)
            }.let { it ?: emptyArray() }.map { file ->
                launch {
                    file.deleteRecursively()
                }
            }.joinAll()
        }
    }

    @Provides
    @IntoMap
    @StringKey("containers")
    fun initializerToMap(
        @Named("containers") task: SuspendLazy<Unit>
    ): SuspendLazy<*> = task
}

