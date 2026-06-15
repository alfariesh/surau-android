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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import org.surau.app.core.designsystem.icon.SurauIcons

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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text(
                    text = stringResource(R.string.home_greeting),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        caption = stringResource(
                            R.string.home_streak_longest, streak.longestStreakDays,
                        ),
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
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_continue_title),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = resume.surahName,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.home_continue_verse, resume.ayahNumber),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            FilledTonalButton(onClick = onClick) {
                Text(stringResource(R.string.home_continue_action))
            }
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
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconBadge(icon)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.headlineSmall)
                Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(SurauIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                IconBadge(SurauIcons.Activity)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.home_khatam_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.home_khatam_progress, completedJuz),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { (percent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SignInCard(onSignIn: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.home_signin_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(R.string.home_signin_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Button(onClick = onSignIn) {
                Text(stringResource(R.string.home_signin_action))
            }
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(48.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
