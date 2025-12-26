package com.github.andock

import android.app.Application
import com.github.andock.daemon.app.AppInitializer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.DelicateCoroutinesApi
import javax.inject.Inject

@OptIn(DelicateCoroutinesApi::class)
@HiltAndroidApp
class AndockApplication : Application() {
    @Inject
    lateinit var initializer: AppInitializer

    override fun onCreate() {
        super.onCreate()
        initializer.onCreate()
    }
}
