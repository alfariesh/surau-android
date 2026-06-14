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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.surau.app.core.designsystem.theme.LocalSurauColors
import java.time.LocalTime

/**
 * A modal time picker presented in an M3 [ModalBottomSheet], wrapping [SurauWheelTimePicker]. Like
 * HeroUI's time picker it **commits live** as the wheels scroll (no confirm button, no title); the
 * sheet closes via swipe-away or scrim tap. Visibility is hoisted by the caller (render only while
 * open).
 *
 * @param value The selected time.
 * @param onValueChange Called live as the wheels settle on a new value.
 * @param onDismiss Called when the sheet is dismissed.
 * @param modifier Modifier applied to the sheet.
 * @param format 12- or 24-hour wheels — see [SurauTimeFormat].
 * @param minuteInterval Step between selectable minutes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurauTimePickerSheet(
    value: LocalTime,
    onValueChange: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    format: SurauTimeFormat = SurauTimeFormat.Hour12,
    minuteInterval: Int = 1,
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
            SurauWheelTimePicker(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.widthIn(max = 240.dp),
                format = format,
                minuteInterval = minuteInterval,
            )
        }
    }
}

/**
 * A read-only trigger field showing the selected time (or a placeholder), styled with HeroUI's
 * Select trigger: a `surface` background, 16dp rounded corners, a soft shadow, and a trailing clock
 * icon. The 1.5dp border is transparent by default and turns `danger` when [isError]. Tapping it
 * should open a [SurauTimePickerSheet].
 *
 * @param value The selected time, or `null` to show [placeholder].
 * @param onClick Called when the field is tapped.
 * @param modifier Modifier applied to the field.
 * @param placeholder Text shown when [value] is `null`.
 * @param enabled When `false`, the field is not clickable.
 * @param isError When `true`, the border uses the danger colour.
 * @param format Controls how the time is formatted (12- vs 24-hour).
 */
@Composable
fun SurauTimePickerField(
    value: LocalTime?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Pilih masa",
    enabled: Boolean = true,
    isError: Boolean = false,
    format: SurauTimeFormat = SurauTimeFormat.Hour12,
) {
    val colors = LocalSurauColors.current
    val shape = RoundedCornerShape(SurauTimePickerDefaults.FieldCornerRadius)
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
            text = value?.let { formatTime(it, format) } ?: placeholder,
            fontSize = 16.sp,
            color = if (value == null) colors.muted else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Rounded.Schedule,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.size(16.dp),
        )
    }
}

private fun formatTime(time: LocalTime, format: SurauTimeFormat): String = when (format) {
    SurauTimeFormat.Hour24 -> "%02d:%02d".format(time.hour, time.minute)
    SurauTimeFormat.Hour12 -> {
        val hour12 = ((time.hour + 11) % 12) + 1
        val period = if (time.hour < 12) "AM" else "PM"
        "%d:%02d %s".format(hour12, time.minute, period)
    }
}

object SurauTimePickerDefaults {
    val FieldCornerRadius = 16.dp
}
