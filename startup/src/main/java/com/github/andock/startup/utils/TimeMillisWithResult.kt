package com.github.andock.startup.utils

data class TimeMillisWithResult<T>(
    val result: T,
    val timeMillis: Long,
)
