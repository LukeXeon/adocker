package com.github.andock.ui.route

import kotlin.reflect.KClass

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class Route(
    val type: KClass<*>,
    val deepLinks: Array<DeepLink> = []
)
