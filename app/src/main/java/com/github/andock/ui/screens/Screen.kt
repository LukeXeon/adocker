package com.github.andock.ui.screens

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry

data class Screen(
    val content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
)