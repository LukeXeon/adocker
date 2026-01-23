package com.github.andock.startup.tasks

import com.github.andock.startup.utils.TimeMillisWithResult

internal fun interface Initializer<T> {
    suspend fun invoke(): TimeMillisWithResult<T>
}