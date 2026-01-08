package com.github.andock.startup

import dagger.MapKey

@MapKey(unwrapValue = false)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class TaskInfo(
    val name: String,
    val trigger: String
)
