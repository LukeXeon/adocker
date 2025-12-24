package com.github.andock.ui2.screens.main

import androidx.compose.runtime.Composable
import kotlin.reflect.KClass

data class Route(
    val type: KClass<*>,
    val screen: @Composable () -> Unit
)