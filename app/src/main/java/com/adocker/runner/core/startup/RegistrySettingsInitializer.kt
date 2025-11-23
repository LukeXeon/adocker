package com.adocker.runner.core.startup

import android.content.Context
import androidx.startup.Initializer
import com.adocker.runner.core.config.RegistrySettings
import timber.log.Timber

/**
 * Initializes RegistrySettings
 */
class RegistrySettingsInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        RegistrySettings.init(context)
        Timber.d("RegistrySettings initialized")
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        // Depends on Config and Timber being initialized first
        return listOf(ConfigInitializer::class.java)
    }
}
