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

package org.surau.app.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import org.surau.app.core.designsystem.component.SurauOtpInput
import org.surau.app.core.designsystem.component.SurauSegmentedControl
import org.surau.app.core.designsystem.component.SurauSwitch
import org.surau.app.core.designsystem.theme.SurauTheme

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
@LooperMode(LooperMode.Mode.PAUSED)
class HeroComponentsInteractionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun switch_togglesOnClick() {
        val checked = mutableStateOf(false)
        composeTestRule.setContent {
            SurauTheme {
                SurauSwitch(
                    checked = checked.value,
                    onCheckedChange = { checked.value = it },
                    modifier = Modifier.testTag("switch"),
                )
            }
        }

        composeTestRule.onNodeWithTag("switch").assertIsOff()
        composeTestRule.onNodeWithTag("switch").performClick()
        composeTestRule.onNodeWithTag("switch").assertIsOn()
        assertEquals(true, checked.value)
    }

    @Test
    fun segmentedControl_selectsTappedSegment() {
        val index = mutableStateOf(0)
        composeTestRule.setContent {
            SurauTheme {
                SurauSegmentedControl(
                    options = listOf("Satu", "Dua", "Tiga"),
                    selectedIndex = index.value,
                    onSelectedIndexChange = { index.value = it },
                )
            }
        }

        composeTestRule.onNodeWithText("Tiga").performClick()
        composeTestRule.runOnIdle { assertEquals(2, index.value) }
    }

    @Test
    fun otpInput_filtersNonDigitsAndReportsFilled() {
        val code = mutableStateOf("")
        val filled = mutableStateOf<String?>(null)
        composeTestRule.setContent {
            SurauTheme {
                SurauOtpInput(
                    value = code.value,
                    onValueChange = { code.value = it },
                    length = 4,
                    onFilled = { filled.value = it },
                    modifier = Modifier.testTag("otp"),
                )
            }
        }

        composeTestRule.onNodeWithTag("otp").performTextInput("12a345")
        composeTestRule.runOnIdle {
            // Non-digits dropped and capped at length.
            assertEquals("1234", code.value)
            assertEquals("1234", filled.value)
        }
    }
}
