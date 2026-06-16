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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
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
import org.surau.app.core.ui.TrackScreenViewEvent
import org.surau.app.feature.activity.impl.ActivityPane

/**
 * The Home tab. A lightweight header (greeting + continue-reading) sits above the full reading
 * activity ([ActivityPane]) — streak, daily chart, heatmap, surah progress and khatam — so the whole
 * dashboard lives on one screen. Guests see a login CTA from [ActivityPane] instead of the activity.
 */
@Composable
fun HomeScreen(
    onContinueReading: (surahId: Int, ayahNumber: Int) -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TrackScreenViewEvent(screenName = "Home")

    ActivityPane(
        onLoginClick = onSignIn,
        modifier = modifier,
        header = {
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
        },
    )
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
