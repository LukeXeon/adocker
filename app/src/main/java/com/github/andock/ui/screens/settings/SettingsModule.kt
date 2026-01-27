package com.github.andock.ui.screens.settings

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {
    @IntoSet
    @Provides
    fun provideFeatureAEntryBuilder(): EntryProviderScope<NavKey>.() -> Unit = {
        entry<SettingsKey> {
            SettingsScreen()
        }
    }
}
