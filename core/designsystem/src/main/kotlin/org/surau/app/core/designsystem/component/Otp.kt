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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.surau.app.core.designsystem.theme.LocalSurauColors

/** Visual style for [SurauOtpInput] cells. */
enum class SurauOtpVariant { Primary, Secondary }

/**
 * A one-time-password input: a single hidden [BasicTextField] drives a row of cells, so there is
 * one focus target and one IME connection (the idiomatic Compose pattern). The active cell shows a
 * blinking caret; entered characters animate in. HeroUI Native `InputOTP`, ported to Compose.
 *
 * @param value The current code.
 * @param onValueChange Called with the filtered code as the user types.
 * @param modifier Modifier applied to the cell row.
 * @param length Number of cells (and the maximum code length).
 * @param enabled When `false`, input is disabled and the cells are dimmed.
 * @param isError When `true`, cell borders use the danger colour.
 * @param variant Cell background style.
 * @param keyboardType IME type. Numeric types restrict input to digits.
 * @param onFilled Called once when the code reaches [length] characters.
 */
@Composable
fun SurauOtpInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = SurauOtpDefaults.LENGTH,
    enabled: Boolean = true,
    isError: Boolean = false,
    variant: SurauOtpVariant = SurauOtpVariant.Primary,
    keyboardType: KeyboardType = KeyboardType.NumberPassword,
    onFilled: (String) -> Unit = {},
) {
    var isFocused by remember { mutableStateOf(false) }
    val isNumeric = keyboardType == KeyboardType.Number ||
        keyboardType == KeyboardType.NumberPassword ||
        keyboardType == KeyboardType.Phone

    BasicTextField(
        value = value,
        onValueChange = { raw ->
            val filtered = raw
                .let { if (isNumeric) it.filter(Char::isDigit) else it.filterNot(Char::isWhitespace) }
                .take(length)
            if (filtered != value) {
                onValueChange(filtered)
                if (filtered.length == length) onFilled(filtered)
            }
        },
        modifier = modifier.onFocusChanged { isFocused = it.isFocused },
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
        // We render our own cells and intentionally do not place the inner text field.
        decorationBox = {
            val activeIndex = if (isFocused && enabled && value.length < length) value.length else -1
            Row(horizontalArrangement = Arrangement.spacedBy(SurauOtpDefaults.CellSpacing)) {
                repeat(length) { index ->
                    OtpCell(
                        char = value.getOrNull(index),
                        isActive = index == activeIndex,
                        isError = isError,
                        enabled = enabled,
                        variant = variant,
                    )
                }
            }
        },
    )
}

@Composable
private fun OtpCell(
    char: Char?,
    isActive: Boolean,
    isError: Boolean,
    enabled: Boolean,
    variant: SurauOtpVariant,
) {
    val colors = LocalSurauColors.current
    val shape = RoundedCornerShape(SurauOtpDefaults.CellCornerRadius)
    val borderColor = when {
        isError -> colors.danger
        isActive -> colors.accent
        else -> colors.fieldBorder
    }
    val background = when (variant) {
        SurauOtpVariant.Primary -> colors.fieldBackground
        SurauOtpVariant.Secondary -> colors.surfaceSecondary
    }
    Box(
        modifier = Modifier
            .size(width = SurauOtpDefaults.CellWidth, height = SurauOtpDefaults.CellHeight)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .clip(shape)
            .background(background)
            .border(width = 1.5.dp, color = borderColor, shape = shape),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = char,
            transitionSpec = {
                (fadeIn(tween(250)) + scaleIn(initialScale = 0.7f)) togetherWith fadeOut(tween(100))
            },
            label = "SurauOtpChar",
        ) { c ->
            when {
                c != null -> Text(
                    text = c.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                isActive -> BlinkingCaret()
                else -> Box(Modifier.size(0.dp))
            }
        }
    }
}

@Composable
private fun BlinkingCaret() {
    val transition = rememberInfiniteTransition(label = "SurauOtpCaret")
    val alpha by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "SurauOtpCaretAlpha",
    )
    val height by transition.animateFloat(
        initialValue = 16f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "SurauOtpCaretHeight",
    )
    Box(
        modifier = Modifier
            .graphicsLayer { this.alpha = alpha }
            .width(2.dp)
            .height(height.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(LocalSurauColors.current.muted),
    )
}

object SurauOtpDefaults {
    const val LENGTH = 6
    val CellWidth = 44.dp
    val CellHeight = 48.dp
    val CellSpacing = 8.dp
    val CellCornerRadius = 12.dp
}
