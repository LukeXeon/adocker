package com.github.andock.ui.screens.qrcode

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object QrcodeModule {
    @IntoSet
    @Provides
    fun provideFeatureAEntryBuilder(): EntryProviderScope<NavKey>.() -> Unit = {
        entry<QrcodeScannerKey> {
            QrcodeScannerScreen()
        }
    }
}
