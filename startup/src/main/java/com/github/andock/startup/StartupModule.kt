package com.github.andock.startup

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StartupModule {
    @Provides
    @Singleton
    fun tasks(tasks: Map<TaskInfo, @JvmSuppressWildcards SuspendLazy<Long>>): Map<String, Map<String, SuspendLazy<Long>>> {
        val map = mutableMapOf<String, MutableMap<String, SuspendLazy<Long>>>()
        tasks.forEach { (key, value) ->
            map.getOrPut(key.trigger) { mutableMapOf() }[key.name] = value
        }
        return map
    }
}