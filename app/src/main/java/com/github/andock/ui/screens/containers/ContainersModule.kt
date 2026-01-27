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
        entry<ContainerCreateKey> {
            ContainerCreateScreen()
        }
        entry<ContainerDetailKey> {
            ContainerDetailScreen()
        }
        entry<ContainerExecKey> {
            ContainerExecScreen()
        }
        entry<ContainerLogKey> {
            ContainerLogScreen()
        }
        entry<ContainersKey> {
            ContainersScreen()
        }
    }
}