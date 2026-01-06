package com.github.andock.ui.screens

import kotlin.reflect.KClass

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class Route(
    val type: KClass<*>
)
