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

package org.surau.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.surau.app.R
import org.surau.app.core.designsystem.component.SurauButton
import org.surau.app.core.designsystem.component.SurauSurface
import org.surau.app.core.designsystem.icon.SurauIcons
import org.surau.app.core.designsystem.theme.SurauTheme

@Composable
fun HomeScreen(
    onContinueReading: (surahId: Int, ayahNumber: Int) -> Unit,
    onSeeActivity: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.home_greeting),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = SurauTheme.colors.muted,
                )
            }
        }

        uiState.continueReading?.let { resume ->
            item {
                ContinueReadingCard(
                    resume = resume,
                    onClick = { onContinueReading(resume.surahId, resume.ayahNumber) },
                )
            }
        }

        if (uiState.signedIn) {
            uiState.streak?.let { streak ->
                item {
                    StatCard(
                        icon = SurauIcons.Streak,
                        title = stringResource(R.string.home_streak_title),
                        value = stringResource(R.string.home_streak_days, streak.currentStreakDays),
                        caption = stringResource(R.string.home_streak_longest, streak.longestStreakDays),
                        onClick = onSeeActivity,
                    )
                }
            }
            uiState.khatam?.let { khatam ->
                item {
                    KhatamCard(
                        completedJuz = khatam.completedJuz.size,
                        percent = khatam.percent.toFloat(),
                        onClick = onSeeActivity,
                    )
                }
            }
        } else {
            item { SignInCard(onSignIn = onSignIn) }
        }
    }
}

@Composable
private fun ContinueReadingCard(
    resume: ContinueReading,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SurauSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(SurauIcons.MenuBook)
            Spacer(Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_continue_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = SurauTheme.colors.muted,
                )
                Text(
                    text = resume.surahName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.home_continue_verse, resume.ayahNumber),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SurauTheme.colors.muted,
                )
            }
            SurauButton(onClick = onClick, text = { Text(stringResource(R.string.home_continue_action)) })
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    title: String,
    value: String,
    caption: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SurauSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconBadge(icon)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = SurauTheme.colors.muted)
                Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(caption, style = MaterialTheme.typography.bodySmall, color = SurauTheme.colors.muted)
            }
            Icon(SurauIcons.ChevronRight, contentDescription = null, tint = SurauTheme.colors.muted)
        }
    }
}

@Composable
private fun KhatamCard(
    completedJuz: Int,
    percent: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SurauSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                IconBadge(SurauIcons.Activity)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.home_khatam_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = SurauTheme.colors.muted,
                    )
                    Text(
                        stringResource(R.string.home_khatam_progress, completedJuz),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { (percent / 100f).coerceIn(0f, 1f) },
                color = SurauTheme.colors.accent,
                trackColor = SurauTheme.colors.default,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SignInCard(onSignIn: () -> Unit, modifier: Modifier = Modifier) {
    SurauSurface(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.home_signin_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                stringResource(R.string.home_signin_body),
                style = MaterialTheme.typography.bodyMedium,
                color = SurauTheme.colors.muted,
            )
            Spacer(Modifier.height(4.dp))
            SurauButton(onClick = onSignIn, text = { Text(stringResource(R.string.home_signin_action)) })
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(SurauTheme.colors.accentSoft, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SurauTheme.colors.accent,
            modifier = Modifier.size(24.dp),
        )
    }
}
