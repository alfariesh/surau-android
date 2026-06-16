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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.surau.app.core.designsystem.theme.LocalSurauColors
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * A three-column scrolling date picker — **day · month · year** (DMY) — sharing a single centre band
 * and edge fade, built from [SurauWheelPicker]'s scrolling column. The day column adapts to the
 * selected month/year (28–31 days, leap years included); changing the month or year clamps the day
 * to the new month's length. Idiomatic Compose counterpart of HeroUI Native Pro's wheel date picker.
 *
 * @param value The selected date.
 * @param onValueChange Called as any column settles on a new value (clamped to a valid date).
 * @param modifier Modifier applied to the picker.
 * @param yearRange Selectable year span (default 1900–2100).
 * @param itemHeight Height of a single row, shared by all columns.
 * @param visibleCount Visible rows per column (odd).
 * @param monthLabel Maps a 1–12 month number to its display label (default: localized short name).
 */
@Composable
fun SurauWheelDatePicker(
    value: LocalDate,
    onValueChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    yearRange: IntRange = SurauWheelDatePickerDefaults.YearRange,
    itemHeight: Dp = SurauWheelPickerDefaults.ItemHeight,
    visibleCount: Int = SurauWheelPickerDefaults.VISIBLE_COUNT,
    monthLabel: (Int) -> String = ::defaultMonthLabel,
) {
    val years = remember(yearRange) { yearRange.toList() }
    val months = remember { (1..12).toList() }

    // Clamp the incoming value into the selectable year span so the wheels never display a year that
    // disagrees with the held value (and the field that hoists it). `withYear` also day-clamps an
    // out-of-range leap day (Feb 29 → Feb 28) on a non-leap target, so the result is always valid.
    val date = remember(value, years) {
        val safeYear = value.year.coerceIn(years.first(), years.last())
        if (safeYear == value.year) value else value.withYear(safeYear)
    }
    // If we had to clamp, reconcile the hoisted state once so the caller/field agree with the wheels.
    LaunchedEffect(date) {
        if (date != value) onValueChange(date)
    }

    val daysInMonth = date.lengthOfMonth()
    val days = remember(daysInMonth) { (1..daysInMonth).toList() }

    val dayIndex = (date.dayOfMonth - 1).coerceIn(0, days.lastIndex)
    val monthIndex = date.monthValue - 1
    val yearIndex = years.indexOf(date.year).coerceAtLeast(0)

    // Commits a column change as a valid date, clamping the day to the (possibly shorter) month.
    fun emit(day: Int = date.dayOfMonth, month: Int = date.monthValue, year: Int = date.year) {
        val safeYear = year.coerceIn(years.first(), years.last())
        val maxDay = YearMonth.of(safeYear, month).lengthOfMonth()
        val safeDay = day.coerceIn(1, maxDay)
        onValueChange(LocalDate.of(safeYear, month, safeDay))
    }

    Box(
        modifier = modifier.height(itemHeight * visibleCount),
        contentAlignment = Alignment.Center,
    ) {
        SurauWheelSelectionBand(itemHeight = itemHeight)
        Row {
            WheelColumn(
                items = days,
                selectedIndex = dayIndex,
                onSelectedIndexChange = { emit(day = days[it.coerceIn(0, days.lastIndex)]) },
                modifier = Modifier.weight(1f),
                visibleCount = visibleCount,
                itemHeight = itemHeight,
                label = { "%02d".format(it) },
            )
            WheelColumn(
                items = months,
                selectedIndex = monthIndex,
                onSelectedIndexChange = { emit(month = months[it.coerceIn(0, months.lastIndex)]) },
                modifier = Modifier.weight(1f),
                visibleCount = visibleCount,
                itemHeight = itemHeight,
                label = { monthLabel(it) },
            )
            WheelColumn(
                items = years,
                selectedIndex = yearIndex,
                onSelectedIndexChange = { emit(year = years[it.coerceIn(0, years.lastIndex)]) },
                modifier = Modifier.weight(1f),
                visibleCount = visibleCount,
                itemHeight = itemHeight,
                label = { it.toString() },
            )
        }
        SurauWheelEdgeFade(visibleCount = visibleCount, itemHeight = itemHeight)
    }
}

/**
 * A modal date picker in an M3 [ModalBottomSheet], wrapping [SurauWheelDatePicker] and committing
 * live as the wheels scroll (no confirm button); the sheet closes via swipe-away or scrim tap.
 *
 * @param value The selected date.
 * @param onValueChange Called live as the wheels settle.
 * @param onDismiss Called when the sheet is dismissed.
 * @param modifier Modifier applied to the sheet.
 * @param yearRange Selectable year span.
 * @param monthLabel Maps a 1–12 month number to its display label (default: localized short name).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurauDatePickerSheet(
    value: LocalDate,
    onValueChange: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    yearRange: IntRange = SurauWheelDatePickerDefaults.YearRange,
    monthLabel: (Int) -> String = ::defaultMonthLabel,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            SurauWheelDatePicker(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.widthIn(max = 300.dp),
                yearRange = yearRange,
                monthLabel = monthLabel,
            )
        }
    }
}

/**
 * A read-only trigger field showing the selected date (or a placeholder), styled like the other
 * Surau field triggers (`surface` background, 16dp rounded, soft shadow, trailing calendar icon).
 * The 1.5dp border is transparent by default and turns `danger` when [isError]. Tap it to open a
 * [SurauDatePickerSheet].
 *
 * @param value The selected date, or `null` to show [placeholder].
 * @param onClick Called when the field is tapped.
 * @param modifier Modifier applied to the field.
 * @param placeholder Text shown when [value] is `null`.
 * @param enabled When `false`, the field is not clickable.
 * @param isError When `true`, the border uses the danger colour.
 * @param monthLabel Maps a 1–12 month number to its display label.
 */
@Composable
fun SurauDatePickerField(
    value: LocalDate?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Pilih tanggal",
    enabled: Boolean = true,
    isError: Boolean = false,
    monthLabel: (Int) -> String = ::defaultMonthLabel,
) {
    val colors = LocalSurauColors.current
    val shape = RoundedCornerShape(SurauWheelDatePickerDefaults.FieldCornerRadius)
    Row(
        modifier = modifier
            .shadow(1.dp, shape)
            .clip(shape)
            .background(colors.surface)
            .border(1.5.dp, if (isError) colors.danger else Color.Transparent, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = value?.let { formatDate(it, monthLabel) } ?: placeholder,
            fontSize = 16.sp,
            color = if (value == null) colors.muted else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Rounded.CalendarMonth,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.size(18.dp),
        )
    }
}

private fun defaultMonthLabel(month: Int): String =
    Month.of(month).getDisplayName(TextStyle.SHORT, Locale.getDefault())

private fun formatDate(date: LocalDate, monthLabel: (Int) -> String): String =
    "${date.dayOfMonth} ${monthLabel(date.monthValue)} ${date.year}"

object SurauWheelDatePickerDefaults {
    val YearRange: IntRange = 1900..2100
    val FieldCornerRadius = 16.dp
}
