package com.github.andock.ui.screens.main

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlin.reflect.KClass

data class MainBottomTab<T : NavKey>(
    val type: KClass<T>,
    @param:StringRes
    val titleResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: () -> T
) {
    companion object {
        inline operator fun <reified T : NavKey> invoke(
            @StringRes
            titleResId: Int,
            selectedIcon: ImageVector,
            unselectedIcon: ImageVector,
            noinline route: () -> T
        ): MainBottomTab<T> {
            return MainBottomTab(T::class, titleResId, selectedIcon, unselectedIcon, route)
        }
    }
}