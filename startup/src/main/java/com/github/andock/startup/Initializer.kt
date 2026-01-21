package com.github.andock.startup

internal fun interface Initializer<T> {
    suspend fun invoke(): TimeMillisWithResult<T>
}