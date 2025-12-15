package com.github.adocker.daemon.app

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

    fun scope(): CoroutineScope

    companion object : AppGlobals by EntryPointAccessors.fromApplication(
        AppContext.application,
        AppGlobals::class.java
    )
}