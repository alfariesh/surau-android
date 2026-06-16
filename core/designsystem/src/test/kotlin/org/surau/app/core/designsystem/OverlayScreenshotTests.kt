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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import org.surau.app.core.designsystem.component.SurauMenuItem
import org.surau.app.core.designsystem.component.SurauOverlayCard
import org.surau.app.core.designsystem.theme.LocalSurauColors
import org.surau.app.core.testing.util.captureMultiTheme

// The popup-anchored overlays render their content in a separate window, which onRoot() can't
// capture; these tests render the styled overlay content card directly (the internal scaffold both
// SurauPopover and SurauDropdownMenu wrap their content in).
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
@LooperMode(LooperMode.Mode.PAUSED)
class OverlayScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun popover_content_multipleThemes() {
        composeTestRule.captureMultiTheme("Popover") {
            Surface {
                Box(modifier = Modifier.padding(16.dp)) {
                    SurauOverlayCard(horizontalPadding = 16.dp, verticalPadding = 12.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Judul popover",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Keterangan singkat di dalam popover.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LocalSurauColors.current.muted,
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun dropdownMenu_content_multipleThemes() {
        composeTestRule.captureMultiTheme("DropdownMenu") {
            Surface {
                Box(modifier = Modifier.padding(16.dp)) {
                    SurauOverlayCard(horizontalPadding = 6.dp, verticalPadding = 12.dp) {
                        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                            SurauMenuItem(text = "Edit", onClick = {})
                            SurauMenuItem(text = "Bagikan", onClick = {})
                            SurauMenuItem(text = "Hapus", onClick = {}, danger = true)
                        }
                    }
                }
            }
        }
    }
}
