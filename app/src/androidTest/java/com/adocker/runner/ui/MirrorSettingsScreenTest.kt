package com.adocker.runner.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.adocker.runner.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class MirrorSettingsScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun init() {
        hiltRule.inject()
        // Navigate to settings -> mirror settings
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Docker Registry Mirror").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun mirrorSettingsScreen_displaysTitle() {
        composeTestRule.apply {
            onNodeWithText("Registry Mirrors").assertIsDisplayed()
        }
    }

    @Test
    fun mirrorSettingsScreen_displaysBuiltInMirrors() {
        composeTestRule.apply {
            onNodeWithText("Docker Hub (Official)").assertIsDisplayed()
            onNodeWithText("Xuanyuan (China)").assertIsDisplayed()
            onNodeWithText("DaoCloud (China)").assertIsDisplayed()
            onNodeWithText("Aliyun (China)").assertIsDisplayed()
            onNodeWithText("Huawei Cloud (China)").assertIsDisplayed()
            // Note: USTC and Tencent Cloud mirrors removed due to connectivity issues in China
        }
    }

    @Test
    fun mirrorSettingsScreen_hasDefaultChip() {
        composeTestRule.apply {
            onNodeWithText("Default").assertIsDisplayed()
        }
    }

    @Test
    fun mirrorSettingsScreen_hasAddButton() {
        composeTestRule.apply {
            onNodeWithText("Add Custom Mirror").assertExists()
        }
    }

    @Test
    fun mirrorSettingsScreen_hasBackButton() {
        composeTestRule.apply {
            onNodeWithContentDescription("Back").assertExists()
        }
    }

    @Test
    fun mirrorSettingsScreen_backButtonNavigates() {
        composeTestRule.apply {
            onNodeWithContentDescription("Back").performClick()
            waitForIdle()

            // Should be back on settings screen
            onNodeWithText("Registry Mirror").assertIsDisplayed()
            onNodeWithText("About").assertIsDisplayed()
        }
    }
}
