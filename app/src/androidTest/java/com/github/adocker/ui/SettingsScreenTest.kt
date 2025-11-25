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
class SettingsScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun init() {
        hiltRule.inject()
        // Navigate to settings
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun settingsScreen_displaysRegistryMirrorSection() {
        composeTestRule.apply {
            onNodeWithText("Registry Mirror").assertIsDisplayed()
            onNodeWithText("Docker Registry Mirror").assertIsDisplayed()
            onNodeWithText("Tap to manage mirrors for faster downloads").assertIsDisplayed()
        }
    }

    @Test
    fun settingsScreen_displaysAboutSection() {
        composeTestRule.apply {
            onNodeWithText("About").assertIsDisplayed()
            onNodeWithText("Version").assertIsDisplayed()
            onNodeWithText("1.0.0").assertIsDisplayed()
            onNodeWithText("App Name").assertIsDisplayed()
            onNodeWithText("ADocker").assertIsDisplayed()
            onNodeWithText("Architecture").assertIsDisplayed()
        }
    }

    @Test
    fun settingsScreen_displaysExecutionEngineSection() {
        composeTestRule.apply {
            onNodeWithText("Execution Engine").assertIsDisplayed()
            onNodeWithText("PRoot Version").assertIsDisplayed()
            onNodeWithText("Default Mode").assertIsDisplayed()
            onNodeWithText("P1 (SECCOMP enabled)").assertIsDisplayed()
        }
    }

    @Test
    fun settingsScreen_displaysStorageSection() {
        composeTestRule.apply {
            // Scroll down to see storage section
            onNodeWithText("Storage").assertExists()
            onNodeWithText("Storage Usage").assertExists()
            onNodeWithText("Data Directory").assertExists()
            onNodeWithText("Clear All Data").assertExists()
        }
    }

    @Test
    fun settingsScreen_mirrorSettingsNavigates() {
        composeTestRule.apply {
            onNodeWithText("Docker Registry Mirror").performClick()
            waitForIdle()
            // Should navigate to mirror settings page
            onNodeWithText("Registry Mirrors").assertIsDisplayed()
        }
    }
}
