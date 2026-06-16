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

import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.surau.app.core.designsystem.component.SurauButton
import org.surau.app.core.designsystem.component.SurauLoadingWheel
import org.surau.app.core.designsystem.component.SurauSegmentedControl
import org.surau.app.core.designsystem.icon.SurauIcons
import org.surau.app.core.designsystem.theme.SurahNameFontFamily
import org.surau.app.core.designsystem.theme.SurauTheme
import org.surau.app.core.designsystem.theme.surahNameGlyphCode
import org.surau.app.core.model.data.quran.JuzSegment
import org.surau.app.core.model.data.quran.RevelationType
import org.surau.app.core.model.data.quran.Surah
import org.surau.app.core.ui.TrackScreenViewEvent

@Composable
fun QuranHomeScreen(
    onSurahClick: (surahId: Int, ayahNumber: Int?) -> Unit,
    onSearchClick: () -> Unit,
    onBookmarksClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuranHomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    QuranHomeScreen(
        uiState = uiState,
        onSurahClick = onSurahClick,
        onSearchClick = onSearchClick,
        onBookmarksClick = onBookmarksClick,
        onSettingsClick = onSettingsClick,
        modifier = modifier,
    )
}

@Composable
internal fun QuranHomeScreen(
    uiState: QuranHomeUiState,
    onSurahClick: (surahId: Int, ayahNumber: Int?) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    onBookmarksClick: () -> Unit = {},
) {
    ReportDrawnWhen { uiState !is QuranHomeUiState.Loading }
    TrackScreenViewEvent(screenName = "QuranHome")

    Column(modifier = modifier.fillMaxSize()) {
        QuranHomeHeader(
            onSearchClick = onSearchClick,
            onBookmarksClick = onBookmarksClick,
            onSettingsClick = onSettingsClick,
        )

        when (uiState) {
            QuranHomeUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("quranHome:loading"),
                    contentAlignment = Alignment.Center,
                ) {
                    SurauLoadingWheel(
                        contentDesc = stringResource(R.string.feature_quran_impl_loading),
                    )
                }
            }

            QuranHomeUiState.Error -> QuranErrorContent(modifier = Modifier.fillMaxSize())

            is QuranHomeUiState.Success -> QuranHomeContent(
                uiState = uiState,
                onSurahClick = onSurahClick,
            )
        }
    }
}

@Composable
private fun QuranHomeHeader(
    onSearchClick: () -> Unit,
    onBookmarksClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.feature_quran_impl_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSearchClick, modifier = Modifier.testTag("quranHome:search")) {
            Icon(
                imageVector = SurauIcons.Search,
                contentDescription = stringResource(R.string.feature_quran_impl_search),
            )
        }
        IconButton(onClick = onBookmarksClick, modifier = Modifier.testTag("quranHome:bookmarks")) {
            Icon(
                imageVector = SurauIcons.Bookmarks,
                contentDescription = stringResource(R.string.feature_quran_impl_bookmarks),
            )
        }
        IconButton(onClick = onSettingsClick, modifier = Modifier.testTag("quranHome:settings")) {
            Icon(
                imageVector = SurauIcons.Settings,
                contentDescription = stringResource(R.string.feature_quran_impl_settings),
            )
        }
    }
}

@Composable
private fun QuranHomeContent(
    uiState: QuranHomeUiState.Success,
    onSurahClick: (surahId: Int, ayahNumber: Int?) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.feature_quran_impl_tab_surah),
        stringResource(R.string.feature_quran_impl_tab_juz),
    )

    Column {
        SurauSegmentedControl(
            options = tabs,
            selectedIndex = selectedTab,
            onSelectedIndexChange = { selectedTab = it },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier.testTag("quranHome:surahList"),
            ) {
                items(uiState.surahs, key = Surah::surahId) { surah ->
                    SurahListItem(
                        surah = surah,
                        progress = uiState.progressBySurah[surah.surahId],
                        onClick = { onSurahClick(surah.surahId, null) },
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.testTag("quranHome:juzList"),
            ) {
                items(uiState.juzList, key = JuzSegment::number) { juz ->
                    JuzListItem(
                        juz = juz,
                        onClick = { onSurahClick(juz.start.surahId, juz.start.ayahNumber) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SurahListItem(
    surah: Surah,
    progress: Float?,
    onClick: () -> Unit,
) {
    ListItem(
        leadingContent = { SurahNumberMedallion(surah.surahId) },
        headlineContent = {
            Text(
                text = surah.nameLatin,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = stringResource(
                        R.string.feature_quran_impl_surah_meta,
                        stringResource(surah.revelationType.labelRes()),
                        surah.ayahCount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // 4.3: thin reading-progress badge (signed-in users only; absent otherwise).
                if (progress != null && progress > 0f) {
                    Spacer(modifier = Modifier.size(4.dp))
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .width(72.dp)
                            .height(4.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .testTag("quranHome:progress:${surah.surahId}"),
                    )
                }
            }
        },
        trailingContent = {
            // The Surah Name font encodes each name as a `surahNNN` ligature, so we render the
            // glyph token rather than the raw Arabic; nameArabic stays as the spoken description.
            Text(
                text = surahNameGlyphCode(surah.surahId),
                fontFamily = SurahNameFontFamily,
                fontSize = 36.sp,
                lineHeight = 36.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { contentDescription = surah.nameArabic },
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .testTag("quranHome:surah:${surah.surahId}")
            .clickable(onClick = onClick),
    )
}

@Composable
private fun JuzListItem(
    juz: JuzSegment,
    onClick: () -> Unit,
) {
    ListItem(
        leadingContent = { SurahNumberMedallion(juz.number) },
        headlineContent = {
            Text(
                text = stringResource(R.string.feature_quran_impl_juz_number, juz.number),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        supportingContent = {
            Text(
                text = stringResource(
                    R.string.feature_quran_impl_juz_range,
                    juz.start.surahName,
                    juz.start.ayahNumber,
                    juz.end.surahName,
                    juz.end.ayahNumber,
                ),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun SurahNumberMedallion(number: Int) {
    Box(
        modifier = Modifier.size(44.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.feature_quran_impl_surah_medallion),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun QuranErrorContent(modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.feature_quran_impl_error_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.feature_quran_impl_error_offline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.size(16.dp))
            SurauButton(onClick = onRetry) {
                Text(stringResource(R.string.feature_quran_impl_retry))
            }
        }
    }
}

internal fun RevelationType.labelRes(): Int = when (this) {
    RevelationType.MAKKIYAH -> R.string.feature_quran_impl_revelation_makkiyah
    RevelationType.MADANIYAH -> R.string.feature_quran_impl_revelation_madaniyah
}

@Preview(showBackground = true)
@Composable
private fun QuranHomePreview() {
    SurauTheme {
        QuranHomeScreen(
            uiState = QuranHomeUiState.Success(
                surahs = listOf(
                    Surah(1, "الفاتحة", "Al-Fatihah", "Pembukaan", RevelationType.MAKKIYAH, 7),
                    Surah(2, "البقرة", "Al-Baqarah", "Sapi Betina", RevelationType.MADANIYAH, 286),
                ),
                juzList = emptyList(),
            ),
            onSurahClick = { _, _ -> },
            onSearchClick = {},
            onSettingsClick = {},
        )
    }
}
