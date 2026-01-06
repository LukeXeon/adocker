package com.github.andock.daemon.registries

import com.github.andock.daemon.database.model.RegistryEntity
import com.github.andock.daemon.database.model.RegistryType
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object RegistryModule {
    const val DEFAULT_REGISTRY = "https://registry-1.docker.io"

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