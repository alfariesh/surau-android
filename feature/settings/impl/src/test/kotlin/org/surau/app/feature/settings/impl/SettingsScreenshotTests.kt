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

package org.surau.app.feature.settings.impl

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.surau.app.core.model.data.DarkThemeConfig
import org.surau.app.core.model.data.auth.AuthState
import org.surau.app.core.model.data.auth.UserSession
import org.surau.app.core.model.data.quran.ReaderMode
import org.surau.app.core.model.data.quran.TranslationSource
import org.surau.app.core.testing.util.captureMultiTheme

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
class SettingsScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun settingsScreen_guest() {
        composeTestRule.captureMultiTheme(
            name = "SettingsGuest",
            shouldCompareDynamicColor = false,
        ) {
            RenderSettings(AuthState.Guest)
        }
    }

    @Test
    fun settingsScreen_authenticated() {
        composeTestRule.captureMultiTheme(
            name = "SettingsAuthenticated",
            shouldCompareDynamicColor = false,
        ) {
            RenderSettings(
                AuthState.Authenticated(
                    UserSession(
                        userId = "user-1",
                        email = "user@surau.org",
                        displayName = "Aisyah",
                        sessionId = "session-1",
                    ),
                ),
            )
        }
    }

    @Composable
    private fun RenderSettings(authState: AuthState) {
        SettingsScreen(
            uiState = successState(authState),
            appVersionName = "0.1.0",
            onBackClick = {},
            onSignInClick = {},
            onLogout = {},
            onChangeDynamicColorPreference = {},
            onChangeDarkThemeConfig = {},
            onChangeReaderMode = {},
            onChangeTranslationSource = {},
            onChangeArabicFontScale = {},
            supportDynamicColor = false,
        )
    }

    private fun successState(authState: AuthState) = SettingsUiState.Success(
        settings = UserEditableSettings(
            useDynamicColor = false,
            darkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
            readerMode = ReaderMode.ARABIC_TRANSLATION,
            translationSourceId = "kemenag-id-translation",
            arabicFontScale = 1.0f,
        ),
        authState = authState,
        translationSources = listOf(
            TranslationSource(
                id = "kemenag-id-translation",
                lang = "id",
                name = "Kemenag",
                translator = "Kementerian Agama RI",
                isDefault = true,
            ),
        ),
    )
}
