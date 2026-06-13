/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.surau.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.surau.app.MainActivity

/**
 * End-to-end navigation smoke tests for the app shell, running against the fake data layer.
 */
@HiltAndroidTest
class NavigationTest {

    /**
     * Manages the components' state and is used to perform injection on your test
     */
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    /**
     * Use the primary activity to initialize the app normally.
     */
    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() = hiltRule.inject()

    /**
     * The welcome screen is shown via a DataStore-driven flow, so it only appears after an async
     * read settles — wait for it before touching the node to avoid a race on first composition.
     */
    private fun awaitWelcomeGuest() = composeTestRule.waitUntil(WELCOME_TIMEOUT_MS) {
        composeTestRule.onAllNodesWithTag("welcome:guest").fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun firstLaunch_showsWelcome_thenGuestLandsOnQuranHome() {
        composeTestRule.apply {
            awaitWelcomeGuest()
            // Fresh install: the welcome screen offers guest reading first.
            onNodeWithTag("welcome:guest").assertIsDisplayed()

            onNodeWithTag("welcome:guest").performClick()

            // Quran home with the fake surah list.
            onNodeWithText("Al-Fatihah").assertIsDisplayed()
        }
    }

    @Test
    fun quranHome_openSurah_readerShowsAyahs_backReturnsHome() {
        composeTestRule.apply {
            awaitWelcomeGuest()
            onNodeWithTag("welcome:guest").performClick()

            onNodeWithText("Al-Fatihah").performClick()
            onNodeWithTag("reader:ayahList").assertIsDisplayed()

            onNodeWithTag("reader:back").performClick()
            onNodeWithText("Al-Fatihah").assertIsDisplayed()
        }
    }

    @Test
    fun quranHome_settings_showsAccountSection_signInOpensLogin() {
        composeTestRule.apply {
            awaitWelcomeGuest()
            onNodeWithTag("welcome:guest").performClick()

            onNodeWithTag("quranHome:settings").performClick()
            onNodeWithTag("settings:signIn").assertIsDisplayed()

            onNodeWithTag("settings:signIn").performClick()
            onNodeWithTag("login:submit").assertIsDisplayed()
        }
    }

    private companion object {
        const val WELCOME_TIMEOUT_MS = 5_000L
    }
}
