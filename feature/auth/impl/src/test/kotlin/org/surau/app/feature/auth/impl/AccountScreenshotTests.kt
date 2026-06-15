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

import androidx.activity.ComponentActivity
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.surau.app.core.model.data.auth.AccountSession
import org.surau.app.core.testing.util.captureMultiTheme

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
class AccountScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun accountHub() {
        composeTestRule.captureMultiTheme(name = "AccountHub", shouldCompareDynamicColor = false) {
            AuthScreenScaffold(
                title = "Kelola Akun",
                onBackClick = {},
                subtitle = "Aisyah",
            ) {
                AccountRow(label = "Profil", onClick = {})
                AccountRow(label = "Ganti kata sandi", onClick = {})
                AccountRow(label = "Ganti email", onClick = {})
                AccountRow(label = "Perangkat & sesi", onClick = {})
                AccountRow(label = "Preferensi email", onClick = {})
                HorizontalDivider()
                AccountRow(label = "Keluar dari semua perangkat", onClick = {})
                AccountRow(
                    label = "Hapus akun",
                    onClick = {},
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    @Test
    fun accountSessions() {
        composeTestRule.captureMultiTheme(name = "AccountSessions", shouldCompareDynamicColor = false) {
            AuthScreenScaffold(
                title = "Perangkat & sesi",
                onBackClick = {},
                subtitle = "Sesi yang sedang aktif di akun Anda.",
            ) {
                SessionRow(session = SESSIONS[0], onRevoke = {})
                HorizontalDivider()
                SessionRow(session = SESSIONS[1], onRevoke = {})
            }
        }
    }

    private companion object {
        val SESSIONS = listOf(
            AccountSession(
                id = "s1",
                userAgent = "Pixel 8 · Android 16",
                clientIp = "203.0.113.1",
                createdAt = null,
                lastUsedAt = null,
                expiresAt = null,
                isCurrent = true,
            ),
            AccountSession(
                id = "s2",
                userAgent = "Chrome · macOS",
                clientIp = "203.0.113.2",
                createdAt = null,
                lastUsedAt = null,
                expiresAt = null,
                isCurrent = false,
            ),
        )
    }
}
