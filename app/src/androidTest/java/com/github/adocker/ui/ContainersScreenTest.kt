package com.github.adocker.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.github.adocker.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ContainersScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun init() {
        hiltRule.inject()
        // Navigate to containers screen - use the bottom nav item
        composeTestRule.onAllNodesWithText("Containers").onFirst().performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun containersScreen_displaysTitle() {
        composeTestRule.apply {
            // Title in top bar
            onAllNodesWithText("Containers").onFirst().assertIsDisplayed()
        }
    }

    @Test
    fun containersScreen_displaysContent() {
        composeTestRule.apply {
            // Either empty state or containers list should be visible
            // Wait for loading to complete
            waitForIdle()
            // Screen should be accessible
            onAllNodesWithText("Containers").onFirst().assertIsDisplayed()
        }
    }
}
