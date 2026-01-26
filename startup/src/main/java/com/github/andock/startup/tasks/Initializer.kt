package com.github.andock.startup.tasks

internal fun interface Initializer<T> {
    suspend operator fun invoke(): TimeMillisWithResult<T>
}