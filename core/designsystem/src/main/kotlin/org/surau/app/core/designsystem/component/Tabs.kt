/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.surau.app.core.designsystem.theme.LocalSurauColors
import org.surau.app.core.designsystem.theme.SurauTheme
import kotlin.math.roundToInt

/**
 * Now in Android tab. Wraps Material 3 [Tab] and shifts text label down.
 *
 * @param selected Whether this tab is selected or not.
 * @param onClick The callback to be invoked when this tab is selected.
 * @param modifier Modifier to be applied to the tab.
 * @param enabled Controls the enabled state of the tab. When `false`, this tab will not be
 * clickable and will appear disabled to accessibility services.
 * @param text The text label content.
 */
@Composable
fun SurauTab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: @Composable () -> Unit,
) {
    Tab(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        text = {
            val style = MaterialTheme.typography.labelLarge.copy(textAlign = TextAlign.Center)
            ProvideTextStyle(
                value = style,
                content = {
                    Box(modifier = Modifier.padding(top = SurauTabDefaults.TabTopPadding)) {
                        text()
                    }
                },
            )
        },
    )
}

/**
 * Now in Android tab row. Wraps Material 3 [TabRow].
 *
 * @param selectedTabIndex The index of the currently selected tab.
 * @param modifier Modifier to be applied to the tab row.
 * @param tabs The tabs inside this tab row. Typically this will be multiple [SurauTab]s. Each element
 * inside this lambda will be measured and placed evenly across the row, each taking up equal space.
 */
@Composable
fun SurauTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    tabs: @Composable () -> Unit,
) {
    TabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                height = 2.dp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        tabs = tabs,
    )
}

@ThemePreviews
@Composable
fun TabsPreview() {
    SurauTheme {
        val titles = listOf("Topics", "People")
        SurauTabRow(selectedTabIndex = 0) {
            titles.forEachIndexed { index, title ->
                SurauTab(
                    selected = index == 0,
                    onClick = { },
                    text = { Text(text = title) },
                )
            }
        }
    }
}

object SurauTabDefaults {
    /** Top padding for the legacy [SurauTab] label. */
    val TabTopPadding = 7.dp

    // [SurauTabs] geometry (HeroUI Native tabs).
    val TrackPadding = 3.dp
    val TrackRadius = 24.dp
    val ItemGap = 4.dp
    val ItemHorizontalPadding = 12.dp
    val ItemVerticalPadding = 6.dp
    val UnderlineThickness = 2.dp
}

/** Visual style for [SurauTabs]. */
enum class SurauTabsVariant { Primary, Secondary }

/**
 * Animated tabs — HeroUI Native's `Tabs`, ported faithfully.
 *
 * [SurauTabsVariant.Primary] is a filled pill group: triggers sit on a `default`-coloured track and
 * a raised `segment` indicator slides behind the selected one (the same construction HeroUI's
 * segmented control is built on). [SurauTabsVariant.Secondary] is an underline: a 2dp `accent` bar
 * beneath the selected trigger over a 1dp `border` baseline. The indicator tracks each trigger's
 * measured bounds and springs (`stiffness 1200, damping 120`).
 *
 * Triggers are natural-width with 4dp spacing (`self-start`). Pass `scrollable = true` for a long
 * row, or constrain width via [modifier]. Sits alongside the simpler [SurauTabRow].
 *
 * @param selectedIndex Index of the selected tab.
 * @param onSelectedIndexChange Called with the index of the tapped tab.
 * @param tabs Tab labels, in order.
 * @param modifier Modifier applied to the tab row.
 * @param variant Indicator style — filled pill (default) or underline.
 * @param scrollable When `true`, the row scrolls horizontally.
 * @param enabled When `false`, tabs are dimmed and not clickable.
 */
@Composable
fun SurauTabs(
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    tabs: List<String>,
    modifier: Modifier = Modifier,
    variant: SurauTabsVariant = SurauTabsVariant.Primary,
    scrollable: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = LocalSurauColors.current
    val density = LocalDensity.current
    val offsets = remember(tabs.size) { mutableStateListOf(*Array(tabs.size) { 0 }) }
    val widths = remember(tabs.size) { mutableStateListOf(*Array(tabs.size) { 0 }) }
    val heights = remember(tabs.size) { mutableStateListOf(*Array(tabs.size) { 0 }) }
    val sel = selectedIndex.coerceIn(0, tabs.lastIndex.coerceAtLeast(0))
    val indicatorSpring = spring<Int>(dampingRatio = 1f, stiffness = 1200f)
    val animOffset by animateIntAsState(offsets.getOrElse(sel) { 0 }, indicatorSpring, "SurauTabsOffset")
    val animWidth by animateIntAsState(widths.getOrElse(sel) { 0 }, indicatorSpring, "SurauTabsWidth")
    // In inspection/preview (paused clock) snap to the measured position so goldens are stable.
    val inspecting = LocalInspectionMode.current
    val drawOffset = if (inspecting) offsets.getOrElse(sel) { 0 } else animOffset
    val drawWidth = if (inspecting) widths.getOrElse(sel) { 0 } else animWidth

    val scrollModifier = if (scrollable) Modifier.horizontalScroll(rememberScrollState()) else Modifier

    when (variant) {
        SurauTabsVariant.Primary -> Box(
            modifier = modifier
                .then(scrollModifier)
                .clip(RoundedCornerShape(SurauTabDefaults.TrackRadius))
                .background(colors.default)
                .padding(SurauTabDefaults.TrackPadding),
        ) {
            val drawHeight = heights.getOrElse(sel) { 0 }
            if (drawWidth > 0 && drawHeight > 0) {
                Box(
                    Modifier
                        .offset { IntOffset(drawOffset, 0) }
                        .width(with(density) { drawWidth.toDp() })
                        .height(with(density) { drawHeight.toDp() })
                        .shadow(
                            elevation = if (enabled) 2.dp else 0.dp,
                            shape = RoundedCornerShape(SurauTabDefaults.TrackRadius),
                        )
                        .clip(RoundedCornerShape(SurauTabDefaults.TrackRadius))
                        .background(colors.segment),
                )
            }
            TabsTriggerRow(
                tabs = tabs,
                selectedIndex = sel,
                enabled = enabled,
                itemShape = RoundedCornerShape(SurauTabDefaults.TrackRadius),
                offsets = offsets,
                widths = widths,
                heights = heights,
                onSelect = onSelectedIndexChange,
            )
        }

        SurauTabsVariant.Secondary -> Box(modifier = modifier.then(scrollModifier)) {
            TabsTriggerRow(
                tabs = tabs,
                selectedIndex = sel,
                enabled = enabled,
                itemShape = RoundedCornerShape(8.dp),
                offsets = offsets,
                widths = widths,
                heights = heights,
                onSelect = onSelectedIndexChange,
            )
            // 1dp baseline under the whole row.
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.border),
            )
            // 2dp accent underline under the selected trigger.
            if (drawWidth > 0) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .offset { IntOffset(drawOffset, 0) }
                        .width(with(density) { drawWidth.toDp() })
                        .height(SurauTabDefaults.UnderlineThickness)
                        .background(if (enabled) colors.accent else colors.muted),
                )
            }
        }
    }
}

@Composable
private fun TabsTriggerRow(
    tabs: List<String>,
    selectedIndex: Int,
    enabled: Boolean,
    itemShape: Shape,
    offsets: MutableList<Int>,
    widths: MutableList<Int>,
    heights: MutableList<Int>,
    onSelect: (Int) -> Unit,
) {
    val colors = LocalSurauColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(SurauTabDefaults.ItemGap)) {
        tabs.forEachIndexed { index, title ->
            val selectedNow = index == selectedIndex
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .onGloballyPositioned {
                        offsets[index] = it.positionInParent().x.roundToInt()
                        widths[index] = it.size.width
                        heights[index] = it.size.height
                    }
                    .clip(itemShape)
                    .clickable(
                        enabled = enabled,
                        interactionSource = interactionSource,
                        indication = null,
                    ) { onSelect(index) }
                    .padding(
                        horizontal = SurauTabDefaults.ItemHorizontalPadding,
                        vertical = SurauTabDefaults.ItemVerticalPadding,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (selectedNow && enabled) {
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
