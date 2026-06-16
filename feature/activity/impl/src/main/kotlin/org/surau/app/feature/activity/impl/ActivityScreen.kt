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

package org.surau.app.feature.activity.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.surau.app.core.designsystem.component.SurauBarChart
import org.surau.app.core.designsystem.component.SurauBarEntry
import org.surau.app.core.designsystem.component.SurauButton
import org.surau.app.core.designsystem.component.SurauLineChart
import org.surau.app.core.designsystem.component.SurauLoadingWheel
import org.surau.app.core.designsystem.component.SurauProgressRing
import org.surau.app.core.designsystem.component.SurauSegmentSize
import org.surau.app.core.designsystem.component.SurauSegmentedControl
import org.surau.app.core.designsystem.component.SurauTextButton
import org.surau.app.core.designsystem.component.SurauTextField
import org.surau.app.core.designsystem.component.SurauWidget
import org.surau.app.core.designsystem.icon.SurauIcons
import org.surau.app.core.designsystem.theme.LocalSurauColors
import org.surau.app.core.designsystem.theme.SurauTheme
import org.surau.app.core.model.data.activity.ReadingActivity
import org.surau.app.core.model.data.activity.ReadingActivityDay
import org.surau.app.core.model.data.activity.ReadingStreak
import org.surau.app.core.model.data.quran.KhatamCycle
import org.surau.app.core.model.data.quran.RevelationType
import org.surau.app.core.model.data.quran.Surah
import org.surau.app.core.ui.TrackScreenViewEvent
import kotlin.math.roundToInt

@Composable
fun ActivityScreen(
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActivityViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.errors.collect { resId ->
            snackbarHostState.showSnackbar(resources.getString(resId))
        }
    }
    ActivityScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onLoginClick = onLoginClick,
        onRetry = viewModel::refresh,
        onStartKhatam = viewModel::startKhatam,
        onMarkJuz = viewModel::markJuz,
        onUnmarkJuz = viewModel::unmarkJuz,
        onCompleteKhatam = viewModel::completeKhatam,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
internal fun ActivityScreen(
    uiState: ActivityUiState,
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit,
    onRetry: () -> Unit,
    onStartKhatam: (String?) -> Unit,
    onMarkJuz: (Int) -> Unit,
    onUnmarkJuz: (Int) -> Unit,
    onCompleteKhatam: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    TrackScreenViewEvent(screenName = "Activity")

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ActivityTopBar(onBackClick = onBackClick)

            when (uiState) {
                ActivityUiState.Loading -> CenteredBox {
                    SurauLoadingWheel(
                        contentDesc = stringResource(R.string.feature_activity_impl_loading),
                    )
                }

                ActivityUiState.LoginRequired -> LoginRequiredContent(onLoginClick = onLoginClick)

                ActivityUiState.Error -> ErrorContent(onRetry = onRetry)

                is ActivityUiState.Success -> ActivityContent(
                    state = uiState,
                    onStartKhatam = onStartKhatam,
                    onMarkJuz = onMarkJuz,
                    onUnmarkJuz = onUnmarkJuz,
                    onCompleteKhatam = onCompleteKhatam,
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

@Composable
private fun ActivityTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 16.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClick, modifier = Modifier.testTag("activity:back")) {
            Icon(
                imageVector = SurauIcons.ArrowBack,
                contentDescription = stringResource(R.string.feature_activity_impl_back),
            )
        }
        Text(
            text = stringResource(R.string.feature_activity_impl_title),
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}

@Composable
private fun ActivityContent(
    state: ActivityUiState.Success,
    onStartKhatam: (String?) -> Unit,
    onMarkJuz: (Int) -> Unit,
    onUnmarkJuz: (Int) -> Unit,
    onCompleteKhatam: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("activity:content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { StreakHeader(streak = state.streak) }
        item { DailyReadingSection(today = state.today, activity = state.activity) }
        item { ActivityHeatmapCard(today = state.today, activity = state.activity) }
        if (state.surahProgress.isNotEmpty()) {
            item { SurahProgressSection(items = state.surahProgress) }
        }
        item {
            KhatamSection(
                khatam = state.khatam,
                juzInFlight = state.juzInFlight,
                completing = state.completing,
                onStartKhatam = onStartKhatam,
                onMarkJuz = onMarkJuz,
                onUnmarkJuz = onUnmarkJuz,
                onCompleteKhatam = onCompleteKhatam,
            )
        }
        if (state.history.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.feature_activity_impl_khatam_history_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(state.history, key = { it.id }) { cycle -> KhatamHistoryRow(cycle) }
        }
    }
}

@Composable
private fun StreakHeader(streak: ReadingStreak) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("activity:streak"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = SurauIcons.Streak,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (streak.activeToday) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
            )
            Column(modifier = Modifier.weight(1f)) {
                // Number and unit on one baseline so a short streak never wraps awkwardly.
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = streak.currentStreakDays.toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.feature_activity_impl_streak_unit),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                Text(
                    text = stringResource(
                        R.string.feature_activity_impl_streak_longest,
                        streak.longestStreakDays,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(
                        if (streak.activeToday) {
                            R.string.feature_activity_impl_streak_active_today
                        } else {
                            R.string.feature_activity_impl_streak_inactive_today
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ActivityHeatmapCard(today: LocalDate, activity: ReadingActivity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.feature_activity_impl_heatmap_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.feature_activity_impl_activity_summary,
                    activity.activeDays,
                    activity.totalQuranAyahs,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            ActivityHeatmap(today = today, countsByDate = activity.quranAyahsByDate)
            if (activity.quranAyahsByDate.values.none { it > 0 }) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.feature_activity_impl_heatmap_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * A GitHub-style heatmap: weekday-labelled, 7 rows (Sun..Sat) × [HEATMAP_WEEKS] columns ending at
 * [today]'s week. Cell intensity comes from the Quran ayahs read that day; tapping a day shows its
 * count, otherwise a legend is shown. Layout/bucketing live in [buildHeatmapColumns] (unit-tested).
 */
@Composable
private fun ActivityHeatmap(
    today: LocalDate,
    countsByDate: Map<LocalDate, Int>,
) {
    val columns = remember(today, countsByDate) { buildHeatmapColumns(today, countsByDate) }
    var selected by remember { mutableStateOf<HeatmapCell?>(null) }
    val monthNames = stringArrayResource(R.array.feature_activity_impl_month_abbrev)

    Column {
        Row {
            Column(verticalArrangement = Arrangement.spacedBy(CELL_GAP)) {
                WEEKDAY_LABELS.forEach { labelRes ->
                    Box(
                        modifier = Modifier
                            .height(CELL_SIZE)
                            .width(28.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (labelRes != 0) {
                            Text(
                                text = stringResource(labelRes),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(CELL_GAP)) {
                columns.forEach { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(CELL_GAP)) {
                        week.forEach { cell ->
                            HeatmapCellBox(
                                cell = cell,
                                selected = cell == selected,
                                onClick = { selected = cell },
                                monthNames = monthNames,
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        val chosen = selected
        if (chosen != null && !chosen.isFuture) {
            Text(
                text = stringResource(
                    R.string.feature_activity_impl_heatmap_day,
                    chosen.date.formatShort(monthNames),
                    chosen.count,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            HeatmapLegend()
        }
    }
}

@Composable
private fun HeatmapCellBox(
    cell: HeatmapCell,
    selected: Boolean,
    onClick: () -> Unit,
    monthNames: Array<String>,
) {
    val color = if (cell.isFuture) Color.Transparent else heatmapLevelColor(cell.level)
    val description = if (cell.isFuture) {
        null
    } else {
        stringResource(
            R.string.feature_activity_impl_heatmap_cell_desc,
            cell.date.formatShort(monthNames),
            cell.count,
        )
    }
    Box(
        modifier = Modifier
            .size(CELL_SIZE)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(color)
            .then(
                if (selected) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.extraSmall)
                } else {
                    Modifier
                },
            )
            .then(if (cell.isFuture) Modifier else Modifier.clickable(onClick = onClick))
            .then(
                if (description != null) {
                    Modifier.semantics { contentDescription = description }
                } else {
                    Modifier
                },
            )
            .testTag(
                if (cell.count > 0 && !cell.isFuture) "activity:heatmap:active" else "activity:heatmap:cell",
            ),
    )
}

@Composable
private fun HeatmapLegend() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.feature_activity_impl_heatmap_legend_less),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        for (level in 0..HEATMAP_MAX_LEVEL) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(heatmapLevelColor(level)),
            )
        }
        Text(
            text = stringResource(R.string.feature_activity_impl_heatmap_legend_more),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun heatmapLevelColor(level: Int): Color = when (level) {
    0 -> MaterialTheme.colorScheme.surfaceVariant
    1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)
    else -> MaterialTheme.colorScheme.primary
}

/**
 * Daily reading activity charted from the per-day Quran ayah deltas (zero-filled). A segmented
 * control switches between a 7-day bar chart and a 30-day trend line; both roll up client-side from
 * the sparse `days[]` series the API returns.
 */
@Composable
private fun DailyReadingSection(today: LocalDate, activity: ReadingActivity) {
    var rangeIndex by remember { mutableStateOf(0) }
    val days = if (rangeIndex == 0) 7 else 30
    val series = remember(today, activity, days) {
        dailySeries(today, activity.quranAyahsByDate, days)
    }
    val months = stringArrayResource(R.array.feature_activity_impl_month_abbrev)
    SurauWidget(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("activity:daily"),
        title = { Text(stringResource(R.string.feature_activity_impl_daily_title)) },
        description = { Text(stringResource(R.string.feature_activity_impl_daily_subtitle)) },
    ) {
        SurauSegmentedControl(
            options = listOf(
                stringResource(R.string.feature_activity_impl_range_7),
                stringResource(R.string.feature_activity_impl_range_30),
            ),
            selectedIndex = rangeIndex,
            onSelectedIndexChange = { rangeIndex = it },
            size = SurauSegmentSize.Sm,
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (rangeIndex == 0) {
            SurauBarChart(
                entries = series.map { (date, count) ->
                    SurauBarEntry(value = count.toFloat(), label = date.dayOfMonth.toString())
                },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            // ~4 evenly-spaced date markers across the 30-day trend.
            val markers = 4
            val labels = (0 until markers).map { k ->
                val index = k * series.lastIndex / (markers - 1)
                series[index].first.formatShort(months)
            }
            SurauLineChart(
                values = series.map { it.second.toFloat() },
                labels = labels,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Surahs the user has started, each with a thin accent progress bar. */
@Composable
private fun SurahProgressSection(items: List<SurahReadProgress>) {
    val colors = LocalSurauColors.current
    SurauWidget(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("activity:surahProgress"),
        title = { Text(stringResource(R.string.feature_activity_impl_surah_progress_title)) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items.forEach { item ->
                val percent = (item.fraction * 100).roundToInt().coerceIn(0, 100)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = item.surah.nameLatin,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = stringResource(
                                R.string.feature_activity_impl_surah_progress_percent,
                                percent,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(colors.default),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(item.fraction.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(colors.accent),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KhatamSection(
    khatam: KhatamCycle?,
    juzInFlight: Set<Int>,
    completing: Boolean,
    onStartKhatam: (String?) -> Unit,
    onMarkJuz: (Int) -> Unit,
    onUnmarkJuz: (Int) -> Unit,
    onCompleteKhatam: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().testTag("activity:khatam")) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.feature_activity_impl_khatam_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (khatam == null) {
                KhatamEmpty(onStartKhatam = onStartKhatam)
            } else {
                KhatamActive(
                    khatam = khatam,
                    juzInFlight = juzInFlight,
                    completing = completing,
                    onMarkJuz = onMarkJuz,
                    onUnmarkJuz = onUnmarkJuz,
                    onCompleteKhatam = onCompleteKhatam,
                )
            }
        }
    }
}

@Composable
private fun KhatamEmpty(onStartKhatam: (String?) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    Column {
        Text(
            text = stringResource(R.string.feature_activity_impl_khatam_none_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        SurauButton(
            onClick = { showDialog = true },
            modifier = Modifier.testTag("activity:khatam:start"),
        ) {
            Text(stringResource(R.string.feature_activity_impl_khatam_start))
        }
    }
    if (showDialog) {
        StartKhatamDialog(
            onConfirm = {
                showDialog = false
                onStartKhatam(it)
            },
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
private fun KhatamActive(
    khatam: KhatamCycle,
    juzInFlight: Set<Int>,
    completing: Boolean,
    onMarkJuz: (Int) -> Unit,
    onUnmarkJuz: (Int) -> Unit,
    onCompleteKhatam: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        SurauProgressRing(
            progress = (khatam.percent / 100.0).toFloat().coerceIn(0f, 1f),
            ringSize = 96.dp,
            strokeWidth = 10.dp,
        ) {
            Text(
                text = "${khatam.completedJuz.size}/${KhatamCycle.TOTAL_JUZ}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(
                    R.string.feature_activity_impl_khatam_percent,
                    khatam.percent.roundToInt(),
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(
                    R.string.feature_activity_impl_khatam_progress,
                    khatam.completedJuz.size,
                    KhatamCycle.TOTAL_JUZ,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    JuzGrid(
        completedJuz = khatam.completedJuz,
        juzInFlight = juzInFlight,
        onMarkJuz = onMarkJuz,
        onUnmarkJuz = onUnmarkJuz,
    )
    Spacer(modifier = Modifier.height(16.dp))
    SurauButton(
        onClick = onCompleteKhatam,
        enabled = khatam.isCompletable && !completing,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("activity:khatam:complete"),
    ) {
        if (completing) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text(stringResource(R.string.feature_activity_impl_khatam_complete))
        }
    }
}

@Composable
private fun JuzGrid(
    completedJuz: Set<Int>,
    juzInFlight: Set<Int>,
    onMarkJuz: (Int) -> Unit,
    onUnmarkJuz: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (rowStart in 1..KhatamCycle.TOTAL_JUZ step JUZ_COLUMNS) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (juz in rowStart until rowStart + JUZ_COLUMNS) {
                    JuzCell(
                        juz = juz,
                        checked = juz in completedJuz,
                        inFlight = juz in juzInFlight,
                        onToggle = { if (juz in completedJuz) onUnmarkJuz(juz) else onMarkJuz(juz) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun JuzCell(
    juz: Int,
    checked: Boolean,
    inFlight: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (checked) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val content = if (checked) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val label = stringResource(R.string.feature_activity_impl_juz_number, juz)
    Box(
        modifier = modifier
            // 48dp is the Material minimum touch target.
            .height(48.dp)
            .clip(MaterialTheme.shapes.small)
            .background(container)
            // Real checkbox semantics so TalkBack announces the marked/unmarked state.
            .toggleable(
                value = checked,
                enabled = !inFlight,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            )
            .alpha(if (inFlight) 0.5f else 1f)
            .semantics { contentDescription = label }
            .testTag("activity:juz:$juz"),
        contentAlignment = Alignment.Center,
    ) {
        if (inFlight) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = content,
            )
        } else {
            Text(
                text = juz.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = content,
            )
        }
    }
}

@Composable
private fun StartKhatamDialog(
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var notes by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feature_activity_impl_khatam_start_dialog_title)) },
        text = {
            SurauTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.feature_activity_impl_khatam_notes_label)) },
                singleLine = true,
                modifier = Modifier.testTag("activity:khatam:notes"),
            )
        },
        confirmButton = {
            SurauTextButton(onClick = { onConfirm(notes.ifBlank { null }) }) {
                Text(stringResource(R.string.feature_activity_impl_khatam_start))
            }
        },
        dismissButton = {
            SurauTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.feature_activity_impl_cancel))
            }
        },
    )
}

@Composable
private fun KhatamHistoryRow(cycle: KhatamCycle) {
    val monthNames = stringArrayResource(R.array.feature_activity_impl_month_abbrev)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = SurauIcons.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(
                R.string.feature_activity_impl_khatam_history_range,
                cycle.startedAt.toDisplayDate(monthNames),
                cycle.completedAt.toDisplayDate(monthNames),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun LoginRequiredContent(onLoginClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag("activity:loginRequired"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = SurauIcons.Streak,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.feature_activity_impl_login_required_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.feature_activity_impl_login_required_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        SurauButton(onClick = onLoginClick, modifier = Modifier.testTag("activity:login")) {
            Text(stringResource(R.string.feature_activity_impl_login_button))
        }
    }
}

@Composable
private fun ErrorContent(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.feature_activity_impl_error_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.feature_activity_impl_error_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        SurauButton(onClick = onRetry) {
            Text(stringResource(R.string.feature_activity_impl_retry))
        }
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("activity:loading"),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Formats an [Instant] as a short local date (e.g. "12 Jun 2026"), or "—" when null. */
private fun Instant?.toDisplayDate(months: Array<String>): String {
    val date = this?.toLocalDateTime(TimeZone.currentSystemDefault())?.date ?: return "—"
    return "${date.dayOfMonth} ${date.monthAbbrev(months)} ${date.year}"
}

/** Short day+month for a [LocalDate] (e.g. "12 Jun"). */
private fun LocalDate.formatShort(months: Array<String>): String = "$dayOfMonth ${monthAbbrev(months)}"

private fun LocalDate.monthAbbrev(months: Array<String>): String =
    months.getOrElse(monthNumber - 1) { monthNumber.toString() }

/** The last [days] days ending at [today] (oldest first), with ayah counts zero-filled. */
private fun dailySeries(
    today: LocalDate,
    countsByDate: Map<LocalDate, Int>,
    days: Int,
): List<Pair<LocalDate, Int>> = (days - 1 downTo 0).map { offset ->
    val date = today.minus(offset, DateTimeUnit.DAY)
    date to (countsByDate[date] ?: 0)
}

private const val JUZ_COLUMNS = 5
private val CELL_SIZE = 28.dp
private val CELL_GAP = 4.dp

// Weekday labels for a Sunday-first column (Sen/Rab/Jum shown, like common heatmaps); 0 = blank.
private val WEEKDAY_LABELS = listOf(
    0,
    R.string.feature_activity_impl_weekday_mon,
    0,
    R.string.feature_activity_impl_weekday_wed,
    0,
    R.string.feature_activity_impl_weekday_fri,
    0,
)

@Preview(showBackground = true)
@Composable
private fun ActivitySuccessPreview() {
    SurauTheme {
        ActivityScreen(
            uiState = ActivityUiState.Success(
                today = LocalDate(2026, 6, 14),
                streak = ReadingStreak(
                    currentStreakDays = 5,
                    longestStreakDays = 12,
                    totalActiveDays = 40,
                    lastActiveDate = LocalDate(2026, 6, 14),
                    activeToday = true,
                ),
                activity = ReadingActivity(
                    from = LocalDate(2026, 5, 10),
                    to = LocalDate(2026, 6, 14),
                    activeDays = 9,
                    totalQuranAyahs = 120,
                    days = listOf(
                        ReadingActivityDay(LocalDate(2026, 6, 14), 12, 0),
                        ReadingActivityDay(LocalDate(2026, 6, 13), 3, 0),
                        ReadingActivityDay(LocalDate(2026, 6, 10), 7, 0),
                    ),
                ),
                khatam = KhatamCycle(
                    id = "c1",
                    startedAt = Instant.parse("2026-06-01T00:00:00Z"),
                    completedAt = null,
                    notes = null,
                    completedJuz = setOf(1, 2, 3, 4, 5),
                    juzCount = 5,
                    percent = 5 * 100.0 / 30,
                ),
                history = emptyList(),
                surahProgress = listOf(
                    SurahReadProgress(
                        Surah(2, "البقرة", "Al-Baqarah", "Sapi Betina", RevelationType.MADANIYAH, 286),
                        0.45f,
                    ),
                    SurahReadProgress(
                        Surah(36, "يس", "Yasin", "Yasin", RevelationType.MAKKIYAH, 83),
                        0.8f,
                    ),
                ),
            ),
            onBackClick = {},
            onLoginClick = {},
            onRetry = {},
            onStartKhatam = {},
            onMarkJuz = {},
            onUnmarkJuz = {},
            onCompleteKhatam = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ActivityLoginRequiredPreview() {
    SurauTheme {
        ActivityScreen(
            uiState = ActivityUiState.LoginRequired,
            onBackClick = {},
            onLoginClick = {},
            onRetry = {},
            onStartKhatam = {},
            onMarkJuz = {},
            onUnmarkJuz = {},
            onCompleteKhatam = {},
        )
    }
}
