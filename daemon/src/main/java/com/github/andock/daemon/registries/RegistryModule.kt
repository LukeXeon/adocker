package com.github.andock.daemon.registries

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
}