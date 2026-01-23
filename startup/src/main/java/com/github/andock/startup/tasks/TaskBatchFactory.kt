package com.github.andock.startup.tasks

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface TaskBatchFactory {
    val newInstance: TaskBatch.NewInstance
}