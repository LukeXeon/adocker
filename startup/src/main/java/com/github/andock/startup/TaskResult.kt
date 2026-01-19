package com.github.andock.startup

class TaskResult(
    val name: String,
    private val times: LongArray,
) {
    val phaseTime: Long
        get() = times[0]

    val totalTime: Long
        get() = times[1]
}