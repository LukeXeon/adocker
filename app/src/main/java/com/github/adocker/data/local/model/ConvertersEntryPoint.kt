package com.github.adocker.data.local.model

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
interface ConvertersEntryPoint {
    fun json(): Json

    companion object {
        val instance = EntryPointAccessors.fromApplication(
            ADockerApplication.instance,
            ConvertersEntryPoint::class.java
        )
    }
}
