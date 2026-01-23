package com.github.andock.startup


@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class Trigger(
    val value: String,
    val processes: Array<String> = [""],
    val dispatcher: DispatcherType = DispatcherType.Default
)