package com.github.andock.startup

data class TimeMillisWithResult<T>(
    val result: T,
    val timeMillis: Long,
)
