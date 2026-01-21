package com.github.andock.startup

internal fun interface SuspendInitializer<T> {
    suspend fun invoke(): TimeMillisWithResult<T>
}