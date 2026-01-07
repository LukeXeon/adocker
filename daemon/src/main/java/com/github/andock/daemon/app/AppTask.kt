package com.github.andock.daemon.app

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
annotation class AppTask(
    val name: String,
    val trigger: String = ""
)
