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

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.surau.app.core.designsystem.theme.LocalSurauColors
import kotlin.math.roundToInt

/** Size preset for [SurauSegmentedControl] (HeroUI Native Pro `Segment` sizes). */
enum class SurauSegmentSize { Sm, Md, Lg }

/**
 * A pill segmented control — HeroUI Native Pro's `Segment` (which is `Tabs` locked to the primary
 * variant). A `default`-coloured track holds a raised `segment`-coloured indicator that springs
 * (`stiffness 1200, damping 120`) to the selected item. Geometry matches the source per size:
 * track padding sm/md/lg = 2/3/4dp, item radius 12/24/22dp, label 14/16/18sp medium.
 *
 * Sizes to its content (segments share the widest item's width); wrap in a width modifier to fix it.
 *
 * @param options The segment labels, in order.
 * @param selectedIndex Index of the selected segment.
 * @param onSelectedIndexChange Called with the index of the tapped segment.
 * @param modifier Modifier applied to the control.
 * @param size Size preset controlling height, padding, radii and typography.
 * @param enabled When `false`, the control is dimmed and not interactive.
 */
@Composable
fun SurauSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    size: SurauSegmentSize = SurauSegmentSize.Md,
    enabled: Boolean = true,
) {
    val colors = LocalSurauColors.current
    val density = LocalDensity.current
    val dims = size.dimensions()

    val offsets = remember(options.size) { mutableStateListOf(*Array(options.size) { 0 }) }
    val widths = remember(options.size) { mutableStateListOf(*Array(options.size) { 0 }) }

    val sel = selectedIndex.coerceIn(0, options.lastIndex.coerceAtLeast(0))
    val indicatorSpring = spring<Int>(dampingRatio = 1f, stiffness = 1200f)
    val animOffset by animateIntAsState(offsets.getOrElse(sel) { 0 }, indicatorSpring, "SurauSegmentOffset")
    val animWidth by animateIntAsState(widths.getOrElse(sel) { 0 }, indicatorSpring, "SurauSegmentWidth")
    val inspecting = LocalInspectionMode.current
    val drawOffset = if (inspecting) offsets.getOrElse(sel) { 0 } else animOffset
    val drawWidth = if (inspecting) widths.getOrElse(sel) { 0 } else animWidth

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(dims.trackRadius))
            .background(colors.default)
            .padding(dims.trackPadding),
    ) {
        // Raised indicator (segment fill) behind the labels.
        if (drawWidth > 0) {
            Box(
                Modifier
                    .offset { IntOffset(drawOffset, 0) }
                    .width(with(density) { drawWidth.toDp() })
                    .height(dims.segmentHeight)
                    .shadow(if (enabled) 2.dp else 0.dp, RoundedCornerShape(dims.indicatorRadius))
                    .clip(RoundedCornerShape(dims.indicatorRadius))
                    .background(colors.segment),
            )
        }
        Row(modifier = Modifier.width(IntrinsicSize.Max)) {
            options.forEachIndexed { index, label ->
                val interactionSource = remember { MutableInteractionSource() }
                val selectedNow = index == sel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(dims.segmentHeight)
                        .onGloballyPositioned {
                            offsets[index] = it.positionInParent().x.roundToInt()
                            widths[index] = it.size.width
                        }
                        .clip(RoundedCornerShape(dims.itemRadius))
                        .clickable(
                            enabled = enabled,
                            interactionSource = interactionSource,
                            indication = null,
                        ) { onSelectedIndexChange(index) }
                        .padding(horizontal = dims.contentPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontSize = dims.fontSize,
                        fontWeight = FontWeight.Medium,
                        color = if (selectedNow) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            colors.muted
                        },
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private data class SegmentDimensions(
    val segmentHeight: Dp,
    val trackPadding: Dp,
    val trackRadius: Dp,
    val itemRadius: Dp,
    val indicatorRadius: Dp,
    val contentPadding: Dp,
    val fontSize: TextUnit,
)

private fun SurauSegmentSize.dimensions(): SegmentDimensions = when (this) {
    // height = lineHeight + 2*verticalPadding; radii/padding per HeroUI Pro size maps.
    SurauSegmentSize.Sm -> SegmentDimensions(28.dp, 2.dp, 24.dp, 12.dp, 24.dp, 10.dp, 14.sp)
    SurauSegmentSize.Md -> SegmentDimensions(36.dp, 3.dp, 24.dp, 24.dp, 24.dp, 12.dp, 16.sp)
    SurauSegmentSize.Lg -> SegmentDimensions(44.dp, 4.dp, 32.dp, 22.dp, 32.dp, 14.dp, 18.sp)
}
