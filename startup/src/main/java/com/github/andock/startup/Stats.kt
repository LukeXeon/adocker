package com.github.andock.startup

import com.github.andock.startup.tasks.TaskResult

data class Stats(
    val name: String,
    val totalTime: Long,
    val tasks: List<TaskResult>
)