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

package org.surau.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.surau.app.core.designsystem.theme.LocalSurauColors
import org.surau.app.core.designsystem.theme.SurauTheme
import java.time.LocalTime

/**
 * `@ThemePreviews` for the HeroUI Pro component set, gathered in one file so the whole family can be
 * previewed (light + dark + dynamic) at a glance. Each component also has golden screenshot tests.
 */

@ThemePreviews
@Composable
fun SurauSwitchPreview() {
    SurauTheme {
        SurauBackground {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SurauSwitch(checked = false, onCheckedChange = {})
                SurauSwitch(checked = true, onCheckedChange = {})
            }
        }
    }
}

@ThemePreviews
@Composable
fun SurauSurfacePreview() {
    SurauTheme {
        SurauBackground {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SurauSurfaceVariant.entries.forEach { variant ->
                    SurauSurface(variant = variant) {
                        Text(text = variant.name, modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}

@ThemePreviews
@Composable
fun SurauSegmentedControlPreview() {
    SurauTheme {
        SurauBackground {
            Box(modifier = Modifier.padding(16.dp)) {
                SurauSegmentedControl(
                    options = listOf("Hari", "Minggu", "Bulan"),
                    selectedIndex = 1,
                    onSelectedIndexChange = {},
                )
            }
        }
    }
}

@ThemePreviews
@Composable
fun SurauTabsPreview() {
    val tabs = listOf("Surah", "Juz", "Penanda")
    SurauTheme {
        SurauBackground {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SurauTabs(0, {}, tabs, variant = SurauTabsVariant.Primary)
                SurauTabs(1, {}, tabs, variant = SurauTabsVariant.Secondary)
            }
        }
    }
}

@ThemePreviews
@Composable
fun SurauWidgetPreview() {
    SurauTheme {
        SurauBackground {
            SurauWidget(
                modifier = Modifier.padding(16.dp),
                title = { Text("Bacaan minggu ini") },
                description = { Text("5 hari berturut-turut") },
                legend = { SurauWidgetLegendItem(label = "Selesai", color = Color(0xFF4F772D)) },
                footer = { Text("Lihat butiran") },
            ) {
                Text(text = "32 ayat", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

@ThemePreviews
@Composable
fun SurauOtpInputPreview() {
    SurauTheme {
        SurauBackground {
            Box(modifier = Modifier.padding(16.dp)) {
                SurauOtpInput(value = "12", onValueChange = {}, length = 4)
            }
        }
    }
}

@ThemePreviews
@Composable
fun SurauToastPreview() {
    SurauTheme {
        SurauBackground {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SurauToastVariant.entries.forEach { variant ->
                    SurauToast(
                        message = variant.name,
                        description = "Mesej toast contoh",
                        variant = variant,
                        onClose = {},
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
fun SurauWheelPickerPreview() {
    SurauTheme {
        SurauBackground {
            SurauWheelPicker(
                items = (1..12).toList(),
                selectedIndex = 3,
                onSelectedIndexChange = {},
                modifier = Modifier.width(96.dp).padding(16.dp),
            )
        }
    }
}

@ThemePreviews
@Composable
fun SurauWheelTimePickerPreview() {
    SurauTheme {
        SurauBackground {
            SurauWheelTimePicker(
                value = LocalTime.of(9, 30),
                onValueChange = {},
                modifier = Modifier.width(240.dp).padding(16.dp),
            )
        }
    }
}

@ThemePreviews
@Composable
fun SurauTimePickerFieldPreview() {
    SurauTheme {
        SurauBackground {
            Box(modifier = Modifier.padding(16.dp)) {
                SurauTimePickerField(
                    value = LocalTime.of(9, 30),
                    onClick = {},
                    modifier = Modifier.width(220.dp),
                )
            }
        }
    }
}

@ThemePreviews
@Composable
fun SurauAlertPreview() {
    SurauTheme {
        SurauBackground {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SurauAlertVariant.entries.forEach { variant ->
                    SurauAlert(
                        title = variant.name,
                        description = "Mesej amaran contoh",
                        variant = variant,
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
fun SurauCellPreview() {
    SurauTheme {
        SurauBackground {
            SurauListGroup(modifier = Modifier.padding(16.dp)) {
                SurauCell(title = "Profil", description = "Urus akaun anda", onClick = {})
                SurauCell(title = "Notifikasi", onClick = {})
                SurauCell(title = "Versi", trailing = { Text("1.0.0") })
            }
        }
    }
}

@ThemePreviews
@Composable
fun SurauSliderPreview() {
    var value by remember { mutableFloatStateOf(40f) }
    SurauTheme {
        SurauBackground {
            Box(modifier = Modifier.padding(16.dp)) {
                SurauSlider(value = value, onValueChange = { value = it })
            }
        }
    }
}

@ThemePreviews
@Composable
fun SurauSelectPreview() {
    SurauTheme {
        SurauBackground {
            Box(modifier = Modifier.padding(16.dp)) {
                SurauSelect(
                    value = "Bahasa Melayu",
                    onValueChange = {},
                    options = listOf("Bahasa Melayu", "English"),
                    modifier = Modifier.width(240.dp),
                )
            }
        }
    }
}

@ThemePreviews
@Composable
fun SurauPopoverContentPreview() {
    SurauTheme {
        SurauBackground {
            Box(modifier = Modifier.padding(16.dp)) {
                SurauOverlayCard(horizontalPadding = 16.dp, verticalPadding = 12.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Tajuk popover",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "Keterangan ringkas di dalam popover.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalSurauColors.current.muted,
                        )
                    }
                }
            }
        }
    }
}

@ThemePreviews
@Composable
fun SurauDropdownMenuContentPreview() {
    SurauTheme {
        SurauBackground {
            Box(modifier = Modifier.padding(16.dp)) {
                SurauOverlayCard(horizontalPadding = 6.dp, verticalPadding = 12.dp) {
                    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                        SurauMenuItem(text = "Sunting", onClick = {})
                        SurauMenuItem(text = "Kongsi", onClick = {})
                        SurauMenuItem(text = "Padam", onClick = {}, danger = true)
                    }
                }
            }
        }
    }
}
