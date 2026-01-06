package com.github.andock.ui.screens.main

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.reflect.KClass

data class MainBottomTab<T : Any>(
    val type: KClass<T>,
    @param:StringRes
    val titleResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: @Composable () -> T
) {
    companion object {
        inline operator fun <reified T : Any> invoke(
            @StringRes
            titleResId: Int,
            selectedIcon: ImageVector,
            unselectedIcon: ImageVector,
            noinline route: @Composable () -> T
        ): MainBottomTab<T> {
            return MainBottomTab(T::class, titleResId, selectedIcon, unselectedIcon, route)
        }
    }
}