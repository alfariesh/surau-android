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
import androidx.compose.foundation.layout.padding
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
import org.surau.app.core.designsystem.component.SurauBarChart
import org.surau.app.core.designsystem.component.SurauBarEntry
import org.surau.app.core.designsystem.component.SurauLineChart
import org.surau.app.core.designsystem.component.SurauProgressRing
import org.surau.app.core.testing.util.captureMultiTheme

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
@LooperMode(LooperMode.Mode.PAUSED)
class ChartScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun progressRing_multipleThemes() {
        composeTestRule.captureMultiTheme("ProgressRing") {
            Surface {
                SurauProgressRing(progress = 0.57f, modifier = Modifier.padding(16.dp)) {
                    Text("57%", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }

    @Test
    fun barChart_multipleThemes() {
        composeTestRule.captureMultiTheme("BarChart") {
            Surface {
                SurauBarChart(
                    entries = listOf(3f, 8f, 2f, 12f, 6f, 0f, 9f).mapIndexed { i, v ->
                        SurauBarEntry(value = v, label = (i + 1).toString())
                    },
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }

    @Test
    fun lineChart_multipleThemes() {
        composeTestRule.captureMultiTheme("LineChart") {
            Surface {
                SurauLineChart(
                    values = listOf(2f, 5f, 3f, 8f, 6f, 11f, 7f, 9f, 4f, 12f),
                    labels = listOf("1", "", "", "5", "", "", "", "", "", "10"),
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
