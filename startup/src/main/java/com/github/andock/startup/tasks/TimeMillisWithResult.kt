package com.github.andock.startup.tasks

data class TimeMillisWithResult<T>(
    val result: T,
    val timeMillis: Long,
)
