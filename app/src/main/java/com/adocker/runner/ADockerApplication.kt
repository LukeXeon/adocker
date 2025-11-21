package com.adocker.runner

import android.app.Application
import com.adocker.runner.core.config.Config
import com.adocker.runner.core.config.RegistrySettings
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ADockerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize config
        Config.init(this)

        // Initialize registry settings with default mirror for China
        RegistrySettings.init(this)
    }

    companion object {
        lateinit var instance: ADockerApplication
            private set
    }
}
