package com.github.andock.startup

import android.os.Handler
import android.os.Looper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object StartupModule {
    @Provides
    @Singleton
    fun tasks(tasks: Map<TaskInfo, TaskComputeTime>): Map<String, Map<String, TaskComputeTime>> {
        val map = mutableMapOf<String, MutableMap<String, TaskComputeTime>>()
        tasks.forEach { (key, value) ->
            map.getOrPut(key.trigger) { mutableMapOf() }[key.name] = value
        }
        return map
    }

    @Named("main-thread")
    @Provides
    @Singleton
    fun mainThread(): Handler {
        return Handler(Looper.getMainLooper())
    }
}