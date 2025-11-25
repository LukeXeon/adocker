package com.github.adocker.core.di

import com.github.adocker.ADockerApplication
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
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

    companion object {
        val instance = EntryPointAccessors.fromApplication(
            ADockerApplication.Companion.instance,
            AppGlobals::class.java
        )
    }
}