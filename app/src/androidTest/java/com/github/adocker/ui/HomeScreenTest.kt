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
class HomeScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun homeScreen_displaysWelcomeMessage() {
        composeTestRule.apply {
            onNodeWithText("Welcome to ADocker").assertIsDisplayed()
            onNodeWithText("Run Docker containers on Android without root").assertIsDisplayed()
        }
    }

    @Test
    fun homeScreen_displaysOverviewSection() {
        composeTestRule.apply {
            onNodeWithText("Overview").assertIsDisplayed()
            // Check for stat cards - use specific numbers that appear in Overview
            onNodeWithText("Running").assertExists()
            onNodeWithText("Stopped").assertExists()
        }
    }

    @Test
    fun homeScreen_displaysQuickActions() {
        composeTestRule.apply {
            onNodeWithText("Quick Actions").assertIsDisplayed()
            onNodeWithText("Pull Image").assertExists()
            onNodeWithText("Download from Docker Hub").assertExists()
            onNodeWithText("View Images").assertExists()
            onNodeWithText("Manage local images").assertExists()
        }
    }
}
