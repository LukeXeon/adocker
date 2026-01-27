package com.github.andock.ui.screens.containers

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object ContainersModule {
    @IntoSet
    @Provides
    fun provideFeatureAEntryBuilder(): EntryProviderScope<NavKey>.() -> Unit = {
        entry<ContainerCreateKey> { key ->
            ContainerCreateScreen(key)
        }
        entry<ContainerDetailKey> { key ->
            ContainerDetailScreen(key)
        }
        entry<ContainerExecKey> { key ->
            ContainerExecScreen(key)
        }
        entry<ContainerLogKey> { key ->
            ContainerLogScreen(key)
        }
        entry<ContainersKey> {
            ContainersScreen()
        }
    }
}