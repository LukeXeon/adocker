package com.github.andock.ui.screens.main

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation3.runtime.NavKey
import kotlin.reflect.KClass

data class MainBottomTab<T : NavKey>(
    val type: KClass<T>,
    @param:StringRes
    val titleResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: @Composable () -> T
) {
    companion object {
        inline operator fun <reified T : NavKey> invoke(
            @StringRes
            titleResId: Int,
            selectedIcon: ImageVector,
            unselectedIcon: ImageVector,
            noinline route: @Composable () -> T
        ): MainBottomTab<T> {
            return MainBottomTab(T::class, titleResId, selectedIcon, unselectedIcon, route)
        }
    }

    @Composable
    inline fun Item(content: (selected: Boolean, route: T) -> Unit) {
        key(type) {
            val navController = LocalNavController.current
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val selected = remember(currentDestination) {
                currentDestination?.hierarchy
                    ?.any { it.hasRoute(type) } == true
            }
            val route = route()
            content(selected, route)
        }
    }
}