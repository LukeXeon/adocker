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
     * Current top-level route (for bottom navigation selection)
     */
    val topLevelRoute: NavKey?
        get() = backStack.lastOrNull { it in topLevelRoutes }

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
        val currentTopLevel = topLevelRoute
        if (currentTopLevel == route) {
            // Same tab - pop to root
            val firstIndex = backStack.indexOfFirst { it == route }
            if (firstIndex >= 0) {
                while (backStack.size > firstIndex + 1) {
                    backStack.removeLast()
                }
            }
        } else {
            // Different tab - find or create the tab stack
            val existingIndex = backStack.indexOfFirst { it == route }
            if (existingIndex >= 0) {
                // Tab exists in stack - restore it (remove everything after current top level, add back this tab's stack)
                val currentTopLevelIndex = backStack.indexOfLast { it in topLevelRoutes && it == currentTopLevel }
                if (currentTopLevelIndex >= 0) {
                    // Keep stack up to current top level
                    while (backStack.size > currentTopLevelIndex + 1) {
                        backStack.removeLast()
                    }
                }
                // Add the new top level route
                backStack.add(route)
            } else {
                // New tab - just add it
                backStack.add(route)
            }
        }
    }

    /**
     * Go back in navigation stack
     */
    fun goBack() {
        if (backStack.size > 1) {
            backStack.removeLast()
        }
    }

    /**
     * Check if we can go back
     */
    fun canGoBack(): Boolean = backStack.size > 1
}
