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

package org.surau.app.feature.settings.impl

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.surau.app.core.model.data.DarkThemeConfig.DARK
import org.surau.app.core.model.data.DarkThemeConfig.FOLLOW_SYSTEM
import org.surau.app.feature.settings.impl.SettingsUiState.Loading
import org.surau.app.feature.settings.impl.SettingsUiState.Success
import org.junit.Rule
import org.junit.Test

class SettingsDialogTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun getString(id: Int) = composeTestRule.activity.resources.getString(id)

    @Test
    fun whenLoading_showsLoadingText() {
        composeTestRule.setContent {
            SettingsDialog(
                settingsUiState = Loading,
                onDismiss = {},
                onChangeDynamicColorPreference = {},
                onChangeDarkThemeConfig = {},
            )
        }

        composeTestRule
            .onNodeWithText(getString(R.string.feature_settings_impl_loading))
            .assertExists()
    }

    @Test
    fun whenStateIsSuccess_allDefaultSettingsAreDisplayed() {
        composeTestRule.setContent {
            SettingsDialog(
                settingsUiState = Success(
                    UserEditableSettings(
                        darkThemeConfig = DARK,
                        useDynamicColor = false,
                    ),
                ),
                onDismiss = {},
                onChangeDynamicColorPreference = {},
                onChangeDarkThemeConfig = {},
            )
        }

        composeTestRule.onNodeWithText(getString(R.string.feature_settings_impl_dark_mode_config_system_default)).assertExists()
        composeTestRule.onNodeWithText(getString(R.string.feature_settings_impl_dark_mode_config_light)).assertExists()
        composeTestRule.onNodeWithText(getString(R.string.feature_settings_impl_dark_mode_config_dark)).assertExists()
        composeTestRule.onNodeWithText(getString(R.string.feature_settings_impl_dark_mode_config_dark)).assertIsSelected()
    }

    @Test
    fun whenStateIsSuccess_supportsDynamicColor_DynamicColorOptionIsDisplayed() {
        composeTestRule.setContent {
            SettingsDialog(
                settingsUiState = Success(
                    UserEditableSettings(
                        darkThemeConfig = FOLLOW_SYSTEM,
                        useDynamicColor = true,
                    ),
                ),
                supportDynamicColor = true,
                onDismiss = {},
                onChangeDynamicColorPreference = {},
                onChangeDarkThemeConfig = {},
            )
        }

        composeTestRule.onNodeWithText(getString(R.string.feature_settings_impl_dynamic_color_preference)).assertExists()
        composeTestRule.onNodeWithText(getString(R.string.feature_settings_impl_dynamic_color_yes)).assertIsSelected()
    }

    @Test
    fun whenStateIsSuccess_notSupportDynamicColor_DynamicColorOptionIsNotDisplayed() {
        composeTestRule.setContent {
            SettingsDialog(
                settingsUiState = Success(
                    UserEditableSettings(
                        darkThemeConfig = FOLLOW_SYSTEM,
                        useDynamicColor = false,
                    ),
                ),
                supportDynamicColor = false,
                onDismiss = {},
                onChangeDynamicColorPreference = {},
                onChangeDarkThemeConfig = {},
            )
        }

        composeTestRule.onNodeWithText(getString(R.string.feature_settings_impl_dynamic_color_preference)).assertDoesNotExist()
    }
}
