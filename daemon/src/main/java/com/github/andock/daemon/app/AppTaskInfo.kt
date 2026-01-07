package com.github.andock.daemon.app

import dagger.MapKey

@MapKey(unwrapValue = false)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class AppTaskInfo(
    val name: String,
    val trigger: String
)
