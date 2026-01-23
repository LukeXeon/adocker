package com.github.andock.startup.tasks

import com.github.andock.startup.DispatcherType
import dagger.MapKey

@MapKey(unwrapValue = false)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class TaskInfo(
    val name: String,
    val trigger: String,
    val processes: Array<String>,
    val dispatcher: DispatcherType
)
