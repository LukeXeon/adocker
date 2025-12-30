package com.github.andock.ui.screens.qrcode

import com.github.andock.ui.screens.Screen
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
object QrcodeModule {

    @Provides
    @IntoMap
    @ClassKey(QrcodeScannerRoute::class)
    fun screen() = Screen {
        QrcodeScannerScreen(it)
    }
}