package com.github.andock.ui.route

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink

data class Screen(
    val deepLinks: List<NavDeepLink> = emptyList(),
    val content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
)