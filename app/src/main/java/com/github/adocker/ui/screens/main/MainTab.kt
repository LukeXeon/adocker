package com.github.adocker.ui.screens.main

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

data class MainTab(
    @param:StringRes
    val titleResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val onCLick: () -> Unit,
    val position: Int
)