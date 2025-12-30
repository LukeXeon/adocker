package com.github.andock.ui.screens.main

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController

data class MainBottomTab(
    @param:StringRes
    val titleResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val priority: Int,
    val onCLick: (NavHostController) -> Unit,
)