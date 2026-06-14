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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.surau.app.core.designsystem.component.SurauButton
import org.surau.app.core.designsystem.component.SurauLoadingWheel
import org.surau.app.core.designsystem.icon.SurauIcons
import org.surau.app.core.designsystem.theme.SurauTheme
import org.surau.app.core.model.data.activity.ReadingActivity
import org.surau.app.core.model.data.activity.ReadingActivityDay
import org.surau.app.core.model.data.activity.ReadingStreak
import org.surau.app.core.model.data.quran.KhatamCycle
import org.surau.app.core.ui.TrackScreenViewEvent

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
        item { ActivityHeatmapCard(today = state.today, activity = state.activity) }
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
                Text(
                    text = stringResource(
                        R.string.feature_activity_impl_streak_days,
                        streak.currentStreakDays,
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
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
            Spacer(modifier = Modifier.height(12.dp))
            ActivityHeatmap(today = today, countsByDate = activity.quranAyahsByDate)
        }
    }
}

/**
 * A GitHub-style heatmap: 7 rows (Sun..Sat) × [WEEKS] columns ending at [today]'s week. Cell
 * intensity is derived from the Quran ayahs read that day; future cells are blank.
 */
@Composable
private fun ActivityHeatmap(
    today: LocalDate,
    countsByDate: Map<LocalDate, Int>,
) {
    val sundayOffset = today.dayOfWeek.isoDayNumber % 7 // Sun -> 0 .. Sat -> 6
    val lastColumnStart = today.minus(sundayOffset, DateTimeUnit.DAY)
    val gridStart = lastColumnStart.minus((WEEKS - 1) * DAYS_PER_WEEK, DateTimeUnit.DAY)
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val filledColor = MaterialTheme.colorScheme.primary

    Row(horizontalArrangement = Arrangement.spacedBy(CELL_GAP)) {
        for (col in 0 until WEEKS) {
            Column(verticalArrangement = Arrangement.spacedBy(CELL_GAP)) {
                for (row in 0 until DAYS_PER_WEEK) {
                    val date = gridStart.plus(col * DAYS_PER_WEEK + row, DateTimeUnit.DAY)
                    val isFuture = date > today
                    val count = countsByDate[date] ?: 0
                    val alpha = intensityAlpha(count)
                    val color = when {
                        isFuture -> Color.Transparent
                        alpha == 0f -> emptyColor
                        else -> filledColor.copy(alpha = alpha)
                    }
                    Box(
                        modifier = Modifier
                            .size(CELL_SIZE)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(color)
                            .testTag(if (count > 0 && !isFuture) "activity:heatmap:active" else "activity:heatmap:cell"),
                    )
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
    Text(
        text = stringResource(
            R.string.feature_activity_impl_khatam_progress,
            khatam.completedJuz.size,
            KhatamCycle.TOTAL_JUZ,
        ),
        style = MaterialTheme.typography.bodyLarge,
    )
    Spacer(modifier = Modifier.height(8.dp))
    LinearProgressIndicator(
        progress = { (khatam.percent / 100.0).toFloat().coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth(),
    )
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
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(MaterialTheme.shapes.small)
            .background(container)
            .clickable(onClick = onToggle)
            .testTag("activity:juz:$juz"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = juz.toString(),
            style = MaterialTheme.typography.titleSmall,
            color = content,
        )
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
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.feature_activity_impl_khatam_notes_label)) },
                singleLine = true,
                modifier = Modifier.testTag("activity:khatam:notes"),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(notes.ifBlank { null }) }) {
                Text(stringResource(R.string.feature_activity_impl_khatam_start))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.feature_activity_impl_cancel))
            }
        },
    )
}

@Composable
private fun KhatamHistoryRow(cycle: KhatamCycle) {
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
                cycle.startedAt.toDisplayDate(),
                cycle.completedAt.toDisplayDate(),
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

private fun intensityAlpha(count: Int): Float = when {
    count <= 0 -> 0f
    count <= 2 -> 0.25f
    count <= 5 -> 0.45f
    count <= 10 -> 0.7f
    else -> 1f
}

/** Formats an [Instant] as a short local date (e.g. "12 Jun 2026"), or "—" when null. */
private fun Instant?.toDisplayDate(): String {
    val date = this?.toLocalDateTime(TimeZone.currentSystemDefault())?.date ?: return "—"
    val month = MONTHS_ID.getOrElse(date.monthNumber - 1) { date.monthNumber.toString() }
    return "${date.dayOfMonth} $month ${date.year}"
}

private val MONTHS_ID = listOf(
    "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
    "Jul", "Agu", "Sep", "Okt", "Nov", "Des",
)

private const val WEEKS = 5
private const val DAYS_PER_WEEK = 7
private const val JUZ_COLUMNS = 5
private val CELL_SIZE = 28.dp
private val CELL_GAP = 4.dp

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
