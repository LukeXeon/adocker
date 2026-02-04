package com.github.andock.startup.tasks

import com.github.andock.common.TimeMillisWithResult

internal fun interface Initializer<T> {
    suspend operator fun invoke(): TimeMillisWithResult<T>
}