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

package org.surau.app.feature.auth.impl

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.surau.app.core.designsystem.component.SurauButton
import org.surau.app.core.testing.util.captureMultiTheme

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
class AuthScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun authForm_loginLayout() {
        composeTestRule.captureMultiTheme(
            name = "AuthLogin",
            shouldCompareDynamicColor = false,
        ) {
            AuthScreenScaffold(
                title = "Masuk ke Surau",
                onBackClick = {},
            ) {
                EmailField(value = "user@surau.org", onValueChange = {}, isError = false)
                Spacer(modifier = Modifier.size(12.dp))
                PasswordField(value = "password123", onValueChange = {}, isError = false)
                Spacer(modifier = Modifier.size(16.dp))
                SurauButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Text("Masuk")
                }
            }
        }
    }

    @Test
    fun authForm_rateLimitedError() {
        composeTestRule.captureMultiTheme(
            name = "AuthRateLimited",
            shouldCompareDarkMode = false,
            shouldCompareDynamicColor = false,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                AuthSubmitError(state = AuthSubmitState.RateLimited(secondsLeft = 42))
                AuthSubmitError(
                    state = AuthSubmitState.Error(AuthErrorKind.INVALID_CREDENTIALS),
                )
            }
        }
    }
}
