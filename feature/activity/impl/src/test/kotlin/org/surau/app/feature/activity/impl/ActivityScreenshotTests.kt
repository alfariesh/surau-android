/*
 * Copyright 2025 The Android Open Source Project
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

package org.surau.app.feature.activity.impl

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.surau.app.core.model.data.activity.ReadingActivity
import org.surau.app.core.model.data.activity.ReadingActivityDay
import org.surau.app.core.model.data.activity.ReadingStreak
import org.surau.app.core.model.data.quran.KhatamCycle
import org.surau.app.core.testing.util.captureMultiTheme

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
class ActivityScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun activity_success() {
        composeTestRule.captureMultiTheme(
            name = "Activity",
            shouldCompareDynamicColor = false,
        ) {
            ActivityPane(
                uiState = sampleSuccess,
                onLoginClick = {},
                onRetry = {},
                onStartKhatam = {},
                onMarkJuz = {},
                onUnmarkJuz = {},
                onCompleteKhatam = {},
            )
        }
    }

    @Test
    fun activity_loginRequired() {
        composeTestRule.captureMultiTheme(
            name = "ActivityLoginRequired",
            shouldCompareDynamicColor = false,
        ) {
            ActivityPane(
                uiState = ActivityUiState.LoginRequired,
                onLoginClick = {},
                onRetry = {},
                onStartKhatam = {},
                onMarkJuz = {},
                onUnmarkJuz = {},
                onCompleteKhatam = {},
            )
        }
    }

    private val sampleSuccess = ActivityUiState.Success(
        today = LocalDate(2026, 6, 14),
        streak = ReadingStreak(
            currentStreakDays = 5,
            longestStreakDays = 12,
            totalActiveDays = 40,
            lastActiveDate = LocalDate(2026, 6, 14),
            activeToday = true,
        ),
        activity = ReadingActivity(
            from = LocalDate(2026, 5, 10),
            to = LocalDate(2026, 6, 14),
            activeDays = 9,
            totalQuranAyahs = 120,
            days = listOf(
                ReadingActivityDay(LocalDate(2026, 6, 14), 12, 0),
                ReadingActivityDay(LocalDate(2026, 6, 13), 3, 0),
                ReadingActivityDay(LocalDate(2026, 6, 12), 7, 0),
                ReadingActivityDay(LocalDate(2026, 6, 9), 1, 0),
                ReadingActivityDay(LocalDate(2026, 6, 2), 20, 0),
            ),
        ),
        khatam = KhatamCycle(
            id = "c1",
            startedAt = Instant.parse("2026-06-01T00:00:00Z"),
            completedAt = null,
            notes = null,
            completedJuz = setOf(1, 2, 3, 4, 5, 6, 7),
            juzCount = 7,
            percent = 7 * 100.0 / 30,
        ),
        history = emptyList(),
    )
}
