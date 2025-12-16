package com.github.adocker.daemon.app

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import org.http4k.server.Http4kServer

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

    fun servers(): Set<@JvmSuppressWildcards Http4kServer>

    companion object : AppGlobals by EntryPointAccessors.fromApplication(
        AppContext.application,
        AppGlobals::class.java
    )
}