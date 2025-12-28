package com.github.andock.daemon.registries

import com.github.andock.daemon.utils.SuspendLazy
import com.github.andock.daemon.utils.suspendLazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object RegistryModule {
    @Provides
    @Singleton
    @IntoSet
    fun initializer(registryManager: RegistryManager): SuspendLazy<*> = suspendLazy {
        registryManager.checkAll()
    }
}