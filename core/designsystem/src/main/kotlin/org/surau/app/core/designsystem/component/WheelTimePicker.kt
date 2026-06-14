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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import java.time.LocalTime
import java.util.Locale

/** Whether the hour column counts 1–12 (with an AM/PM column) or 0–23. */
enum class SurauTimeFormat { Hour12, Hour24 }

/**
 * A three-column (or two, in 24-hour mode) scrolling time picker — hour, minute and an optional
 * AM/PM column — sharing a single centre band and edge fade. Idiomatic Compose port of the HeroUI
 * Native wheel time picker, built from [SurauWheelPicker]'s scrolling column.
 *
 * @param value The selected time.
 * @param onValueChange Called as any column settles on a new value.
 * @param modifier Modifier applied to the picker.
 * @param format [SurauTimeFormat.Hour12] (default) shows an AM/PM column; [SurauTimeFormat.Hour24]
 *        shows hours 0–23 and no period column.
 * @param minuteInterval Step between selectable minutes (e.g. `5` ⇒ 0, 5, 10 …). Must divide 60.
 * @param itemHeight Height of a single row, shared by all columns.
 * @param visibleCount Visible rows per column (odd).
 */
@Composable
fun SurauWheelTimePicker(
    value: LocalTime,
    onValueChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    format: SurauTimeFormat = SurauTimeFormat.Hour12,
    minuteInterval: Int = 1,
    itemHeight: Dp = SurauWheelPickerDefaults.ItemHeight,
    visibleCount: Int = SurauWheelPickerDefaults.VISIBLE_COUNT,
) {
    require(60 % minuteInterval == 0) { "minuteInterval must divide 60, was $minuteInterval" }
    val is24 = format == SurauTimeFormat.Hour24

    val hours = remember(is24) { if (is24) (0..23).toList() else (1..12).toList() }
    val minutes = remember(minuteInterval) { (0 until 60 step minuteInterval).toList() }
    val periods = remember { listOf("AM", "PM") }

    // Decompose the current value into per-column indices.
    val hourIndex = if (is24) value.hour else (((value.hour + 11) % 12) + 1) - 1
    val minuteIndex = (value.minute / minuteInterval).coerceIn(0, minutes.lastIndex)
    val periodIndex = if (value.hour < 12) 0 else 1

    fun emit(hIdx: Int = hourIndex, mIdx: Int = minuteIndex, pIdx: Int = periodIndex) {
        val minute = minutes[mIdx.coerceIn(0, minutes.lastIndex)]
        val hour24 = if (is24) {
            hours[hIdx.coerceIn(0, hours.lastIndex)]
        } else {
            val hour12 = hours[hIdx.coerceIn(0, hours.lastIndex)] // 1..12
            val pm = pIdx == 1
            when {
                hour12 == 12 && !pm -> 0
                hour12 == 12 && pm -> 12
                pm -> hour12 + 12
                else -> hour12
            }
        }
        onValueChange(LocalTime.of(hour24, minute))
    }

    Box(
        modifier = modifier.height(itemHeight * visibleCount),
        contentAlignment = Alignment.Center,
    ) {
        SurauWheelSelectionBand(itemHeight = itemHeight)
        Row {
            WheelColumn(
                items = hours,
                selectedIndex = hourIndex,
                onSelectedIndexChange = { emit(hIdx = it) },
                modifier = Modifier.weight(1f),
                visibleCount = visibleCount,
                itemHeight = itemHeight,
                label = { if (is24) "%02d".format(it) else it.toString() },
            )
            WheelColumn(
                items = minutes,
                selectedIndex = minuteIndex,
                onSelectedIndexChange = { emit(mIdx = it) },
                modifier = Modifier.weight(1f),
                visibleCount = visibleCount,
                itemHeight = itemHeight,
                label = { "%02d".format(it) },
            )
            if (!is24) {
                WheelColumn(
                    items = periods,
                    selectedIndex = periodIndex,
                    onSelectedIndexChange = { emit(pIdx = it) },
                    modifier = Modifier.weight(1f),
                    visibleCount = visibleCount,
                    itemHeight = itemHeight,
                    label = { it.uppercase(Locale.US) },
                )
            }
        }
        SurauWheelEdgeFade(visibleCount = visibleCount, itemHeight = itemHeight)
    }
}
