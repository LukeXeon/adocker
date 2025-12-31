package com.github.andock.daemon.os

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OsModule {
    @Provides
    @Singleton
    fun locator(): ProcessLocator {
        Runtime.getRuntime().runCatching {
            exec("echo").apply {
                destroy()
            }.javaClass.getDeclaredField("pid").apply {
                isAccessible = true
            }
        }.onFailure { e ->
            Timber.d(e)
        }.fold(
            { field ->
                return object : ProcessLocator.Reflection() {
                    override fun getField(process: Process): Int {
                        return field.runCatching {
                            getInt(process)
                        }.getOrDefault(0)
                    }
                }
            },
            {
                return ProcessLocator.Parser
            }
        )
    }
}