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

package org.surau.app.feature.quran.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.surau.app.core.data.repository.QuranSearchResult
import org.surau.app.core.designsystem.component.AyahText
import org.surau.app.core.designsystem.component.SurauLoadingWheel
import org.surau.app.core.designsystem.icon.SurauIcons
import org.surau.app.core.ui.TrackScreenViewEvent

@Composable
fun QuranSearchScreen(
    onBackClick: () -> Unit,
    onResultClick: (surahId: Int, ayahNumber: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuranSearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    QuranSearchScreen(
        uiState = uiState,
        query = query,
        onQueryChanged = viewModel::onQueryChanged,
        onBackClick = onBackClick,
        onResultClick = onResultClick,
        modifier = modifier,
    )
}

@Composable
internal fun QuranSearchScreen(
    uiState: QuranSearchUiState,
    query: String,
    onQueryChanged: (String) -> Unit,
    onBackClick: () -> Unit,
    onResultClick: (surahId: Int, ayahNumber: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    TrackScreenViewEvent(screenName = "QuranSearch")
    val focusRequester = remember { FocusRequester() }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = SurauIcons.ArrowBack,
                    contentDescription = stringResource(R.string.feature_quran_impl_back),
                )
            }
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChanged,
                placeholder = { Text(stringResource(R.string.feature_quran_impl_search_hint)) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .testTag("quranSearch:input"),
            )
        }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        when (uiState) {
            QuranSearchUiState.EmptyQuery -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.feature_quran_impl_search_min_chars),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            QuranSearchUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SurauLoadingWheel(
                        contentDesc = stringResource(R.string.feature_quran_impl_loading),
                    )
                }
            }

            QuranSearchUiState.Error -> QuranErrorContent(modifier = Modifier.fillMaxSize())

            is QuranSearchUiState.Success -> {
                if (uiState.isEmpty) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(
                                R.string.feature_quran_impl_search_empty,
                                uiState.query,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.testTag("quranSearch:results")) {
                        if (uiState.isOffline) {
                            item(key = "offlineBanner") {
                                OfflineResultsBanner()
                            }
                        }
                        items(
                            uiState.results,
                            key = { it.ayah.ayah.ayahKey.value },
                        ) { result ->
                            SearchResultItem(result = result, onClick = onResultClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineResultsBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = SurauIcons.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = stringResource(R.string.feature_quran_impl_search_offline),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SearchResultItem(
    result: QuranSearchResult,
    onClick: (surahId: Int, ayahNumber: Int) -> Unit,
) {
    val ayah = result.ayah.ayah
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(ayah.surahId, ayah.ayahNumber) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = "QS ${ayah.ayahKey.value}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        AyahText(
            text = ayah.textQpcHafs,
            fontScale = 0.9f,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp),
        )
        result.ayah.translation?.let { translation ->
            Text(
                text = translation.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
    }
}
