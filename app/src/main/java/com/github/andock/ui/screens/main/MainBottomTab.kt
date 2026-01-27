package com.github.andock.ui.screens.main

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey

data class MainBottomTab<T : NavKey>(
    @param:StringRes
    val titleResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: T
)