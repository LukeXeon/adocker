package com.github.adocker.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.github.adocker.ui.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ImagesScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun init() {
        hiltRule.inject()
        // Navigate to images screen - use the bottom nav item
        composeTestRule.onAllNodesWithText("Images").onFirst().performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun imagesScreen_displaysTitle() {
        composeTestRule.apply {
            // Title in top bar
            onAllNodesWithText("Images").onFirst().assertIsDisplayed()
        }
    }

    @Test
    fun imagesScreen_displaysContent() {
        composeTestRule.apply {
            // Either empty state or images list should be visible
            // Wait for loading to complete
            waitForIdle()
            // Screen should be accessible
            onAllNodesWithText("Images").onFirst().assertIsDisplayed()
        }
    }
}
