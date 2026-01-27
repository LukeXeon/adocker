package com.github.andock.ui.screens.limits

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object LimitsModule {
    @IntoSet
    @Provides
    fun provideFeatureAEntryBuilder(): EntryProviderScope<NavKey>.() -> Unit = {
        entry<ProcessLimitKey> {
            ProcessLimitScreen()
        }
    }
}
