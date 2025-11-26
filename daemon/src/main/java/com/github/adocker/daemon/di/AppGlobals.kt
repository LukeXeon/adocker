package com.github.adocker.daemon.di

import android.annotation.SuppressLint
import android.app.Application
import com.github.adocker.daemon.containers.PRootEngine
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json

/**
 * EntryPoint for accessing Hilt dependencies in Room TypeConverter
 *
 * Room TypeConverters cannot use constructor injection, so we need
 * to use EntryPoint to access Hilt-provided dependencies.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppGlobals {
    fun json(): Json

    @SuppressLint("PrivateApi")
    companion object {
        operator fun invoke(): AppGlobals {
            return EntryPointAccessors.fromApplication(
                application,
                AppGlobals::class.java
            )
        }

        val application by lazy(LazyThreadSafetyMode.PUBLICATION) {
            Class.forName("android.app.ActivityThread")
                .getDeclaredMethod(
                    "currentApplication"
                ).apply {
                    isAccessible = true
                }.invoke(null) as Application
        }
    }
}