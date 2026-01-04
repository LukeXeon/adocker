package com.github.andock.daemon.registries

import com.github.andock.daemon.database.model.RegistryEntity
import com.github.andock.daemon.database.model.RegistryType
import com.github.andock.daemon.utils.SuspendLazy
import com.github.andock.daemon.utils.suspendLazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import javax.inject.Named
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object RegistryModule {
    const val DEFAULT_REGISTRY = "https://registry-1.docker.io"

    @Provides
    @Singleton
    @Named("registry")
    fun initializer(
        registryManager: RegistryManager,
        @Named("logging")
        logging: SuspendLazy<Unit>,
        @Named("reporter")
        reporter: SuspendLazy<Unit>
    ) = suspendLazy {
        logging.getValue()
        reporter.getValue()
        registryManager.checkAll()
    }

    @Provides
    @IntoMap
    @StringKey("registry")
    fun initializerToMap(
        @Named("registry")
        task: SuspendLazy<Unit>
    ): SuspendLazy<*> = task

    @Provides
    @Singleton
    fun builtinServers() = listOf(
        RegistryEntity(
            id = "c0d1e2f3-4567-49ab-cdef-0123456789ab",
            name = "Docker Hub",
            url = DEFAULT_REGISTRY,
            priority = 100,
            tags = listOf("Official"),
            type = RegistryType.Official,
        ),
        RegistryEntity(
            id = "3f8e7d6c-5b4a-4876-80fe-dcba98765432",
            name = "DaoCloud",
            url = "https://docker.m.daocloud.io",
            priority = 100,
            tags = listOf("China"),
            type = RegistryType.BuiltinMirror,
        ),
    )
}