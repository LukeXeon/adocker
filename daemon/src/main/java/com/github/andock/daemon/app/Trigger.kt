package com.github.andock.daemon.app


@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class Trigger(
    val value: String,
)