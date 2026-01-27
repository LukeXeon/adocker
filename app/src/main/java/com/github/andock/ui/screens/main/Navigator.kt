package com.github.andock.ui.screens.main

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey

/**
 * Navigator handles navigation logic for Navigation 3.
 * Replaces NavController from Navigation 2.
 */
class Navigator(
    private val backStack: SnapshotStateList<NavKey>,
    private val topLevelRoutes: Set<NavKey>
) {
    /**
     * Current route (last item in backstack)
     */
    val currentRoute: NavKey?
        get() = backStack.lastOrNull()

    /**
     * Navigate to a destination.
     * For top-level routes (bottom tabs), clears to that route.
     * For child routes, adds to the stack.
     */
    fun navigate(route: NavKey) {
        if (route in topLevelRoutes) {
            // Navigate to top-level route
            navigateToTopLevel(route)
        } else {
            // Navigate to child route - add to stack
            if (backStack.isEmpty() || backStack.last() != route) {
                backStack.add(route)
            }
        }
    }

    /**
     * Navigate to a top-level route with save/restore behavior
     */
    private fun navigateToTopLevel(route: NavKey) {
        if (backStack[0] == route) {
            backStack.removeRange(1, backStack.size)
        } else {
            backStack.clear()
            backStack.add(route)
        }
    }

    /**
     * Go back in navigation stack
     */
    fun goBack() {
        when {
            backStack.size > 1 -> {
                backStack.removeLastOrNull()
            }
        }
    }

    /**
     * Check if we can go back
     */
    fun canGoBack(): Boolean {
        return when {
            backStack.size > 1 -> {
                true
            }

            else -> {
                false
            }
        }
    }
}
