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

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.surau.app.core.designsystem.component.SurauButton
import org.surau.app.core.designsystem.component.SurauTextButton
import org.surau.app.core.designsystem.theme.SeedPaletteStyle
import org.surau.app.core.designsystem.theme.SeedSchemeReport
import org.surau.app.core.designsystem.theme.seedScheme
import org.surau.app.core.model.data.ThemeContrast
import org.surau.app.core.model.data.ThemeStyle

/** Packs an opaque [Color] into the ARGB Long the theme seed is stored as. */
internal fun Color.toThemeArgb(): Long = (toArgb().toLong() and 0xFFFFFFFFL) or 0xFF000000L

/** Maps the persisted [ThemeStyle] to the design-system generator style. */
internal fun ThemeStyle.toSeedPaletteStyle(): SeedPaletteStyle = when (this) {
    ThemeStyle.TONAL_SPOT -> SeedPaletteStyle.TONAL_SPOT
    ThemeStyle.VIBRANT -> SeedPaletteStyle.VIBRANT
    ThemeStyle.EXPRESSIVE -> SeedPaletteStyle.EXPRESSIVE
    ThemeStyle.NEUTRAL -> SeedPaletteStyle.NEUTRAL
}

/** Maps the persisted [ThemeContrast] to the generator's 0f..1f contrast level. */
internal fun ThemeContrast.toContrastLevel(): Float = when (this) {
    ThemeContrast.STANDARD -> 0f
    ThemeContrast.MEDIUM -> 0.5f
    ThemeContrast.HIGH -> 1f
}

/** Converts an sRGB [Color] to `[hue (0..360), saturation (0..1), value (0..1)]`. */
private fun Color.toHsv(): FloatArray {
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    var hue = when {
        delta == 0f -> 0f
        max == r -> 60f * (((g - b) / delta) % 6f)
        max == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    if (hue < 0f) hue += 360f
    val saturation = if (max == 0f) 0f else delta / max
    return floatArrayOf(hue, saturation, max)
}

/**
 * A modal bottom sheet that lets the user pick any color (HSV square + hue bar) and see, live, how
 * the generated theme will look in both light and dark — including the real accent-on-surface
 * contrast and a heads-up when the color had to be nudged for legibility. Writes happen only on
 * "Apply", so dragging never thrashes the DataStore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeColorPickerSheet(
    initial: Color,
    style: SeedPaletteStyle,
    contrast: Float,
    onApply: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val initialHsv = remember(initial) { initial.toHsv() }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1].coerceAtLeast(0.15f)) }
    var value by remember { mutableFloatStateOf(initialHsv[2].coerceAtLeast(0.25f)) }

    val picked = Color.hsv(hue, saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val lightReport = remember(picked, style, contrast) { seedScheme(picked, false, style, contrast) }
    val darkReport = remember(picked, style, contrast) { seedScheme(picked, true, style, contrast) }
    val currentReport = if (isDark) darkReport else lightReport

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.feature_settings_impl_theme_custom_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.size(16.dp))

            SaturationValueArea(
                hue = hue,
                saturation = saturation,
                value = value,
                onChange = { s, v ->
                    saturation = s
                    value = v
                },
            )
            Spacer(Modifier.size(12.dp))
            HueBar(hue = hue, onHueChange = { hue = it })

            Spacer(Modifier.size(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PreviewSwatch(
                    report = lightReport,
                    label = stringResource(R.string.feature_settings_impl_theme_preview_light),
                    modifier = Modifier.weight(1f),
                )
                PreviewSwatch(
                    report = darkReport,
                    label = stringResource(R.string.feature_settings_impl_theme_preview_dark),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.size(12.dp))
            ContrastReadout(currentReport)
            if (currentReport.adjusted) {
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.feature_settings_impl_theme_adjusted),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.size(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SurauTextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.feature_settings_impl_cancel))
                }
                Spacer(Modifier.weight(1f))
                SurauButton(
                    onClick = { onApply(picked) },
                    modifier = Modifier.testTag("settings:themeApply"),
                ) {
                    Text(stringResource(R.string.feature_settings_impl_theme_apply))
                }
            }
        }
    }
}

@Composable
private fun SaturationValueArea(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (Float, Float) -> Unit,
) {
    val hueColor = Color.hsv(hue, 1f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.4f)
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onChange(
                        (offset.x / size.width).coerceIn(0f, 1f),
                        (1f - offset.y / size.height).coerceIn(0f, 1f),
                    )
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onChange(
                        (change.position.x / size.width).coerceIn(0f, 1f),
                        (1f - change.position.y / size.height).coerceIn(0f, 1f),
                    )
                }
            }
            .drawBehind {
                drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor)))
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                val cx = saturation.coerceIn(0f, 1f) * size.width
                val cy = (1f - value.coerceIn(0f, 1f)) * size.height
                drawCircle(Color.Black.copy(alpha = 0.5f), 9.dp.toPx(), Offset(cx, cy), style = Stroke(4f))
                drawCircle(Color.White, 9.dp.toPx(), Offset(cx, cy), style = Stroke(2f))
            },
    )
}

@Composable
private fun HueBar(hue: Float, onHueChange: (Float) -> Unit) {
    val hueColors = remember {
        (0..360 step 30).map { Color.hsv(it.toFloat(), 1f, 1f) }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onHueChange((offset.x / size.width).coerceIn(0f, 1f) * 360f)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onHueChange((change.position.x / size.width).coerceIn(0f, 1f) * 360f)
                }
            }
            .drawBehind {
                drawRect(Brush.horizontalGradient(hueColors))
                val cx = (hue / 360f).coerceIn(0f, 1f) * size.width
                val r = size.height / 2f
                drawCircle(Color.Black.copy(alpha = 0.5f), r, Offset(cx, r), style = Stroke(4f))
                drawCircle(Color.White, r, Offset(cx, r), style = Stroke(2f))
            },
    )
}

@Composable
private fun PreviewSwatch(
    report: SeedSchemeReport,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(report.scheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(report.scheme.primary),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Aa",
                    style = MaterialTheme.typography.titleMedium,
                    color = report.scheme.onSurface,
                )
            }
        }
        Spacer(Modifier.size(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ContrastReadout(report: SeedSchemeReport) {
    val ratio = report.accentOnSurface
    val grade = when {
        ratio >= 7f -> stringResource(R.string.feature_settings_impl_theme_grade_aaa)
        ratio >= 4.5f -> stringResource(R.string.feature_settings_impl_theme_grade_aa)
        else -> stringResource(R.string.feature_settings_impl_theme_grade_low)
    }
    Text(
        text = stringResource(
            R.string.feature_settings_impl_theme_contrast_readout,
            formatRatio(ratio),
            grade,
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Start,
    )
}

/** One-decimal contrast ratio without locale float artifacts. */
private fun formatRatio(ratio: Float): String {
    val tenths = (ratio * 10f).toInt()
    return "${tenths / 10}.${tenths % 10}"
}
