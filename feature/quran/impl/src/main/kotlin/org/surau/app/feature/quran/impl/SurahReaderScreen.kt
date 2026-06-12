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

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package org.surau.app.feature.quran.impl

import android.content.Intent
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import org.surau.app.core.designsystem.component.AyahText
import org.surau.app.core.designsystem.component.SurauButtonGroup
import org.surau.app.core.designsystem.component.SurauLoadingWheel
import org.surau.app.core.designsystem.icon.SurauIcons
import org.surau.app.core.domain.ReaderContent
import org.surau.app.core.model.data.quran.PopulatedAyah
import org.surau.app.core.model.data.quran.ReaderMode
import org.surau.app.core.model.data.quran.TranslationSource
import org.surau.app.core.ui.TrackScreenViewEvent
import org.surau.app.core.ui.TrackScrollJank
import org.surau.app.feature.quran.api.navigation.SurahReaderNavKey

@Composable
fun SurahReaderScreen(
    navKey: SurahReaderNavKey,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SurahReaderViewModel =
        hiltViewModel<SurahReaderViewModel, SurahReaderViewModel.Factory>(
            key = navKey.toString(),
        ) { factory ->
            factory.create(navKey)
        },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val translationSources by viewModel.translationSources.collectAsStateWithLifecycle()
    SurahReaderScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onAyahVisible = viewModel::onAyahVisible,
        onReaderModeChange = viewModel::setReaderMode,
        onFontScaleChange = viewModel::setArabicFontScale,
        onTranslationSourceChange = viewModel::setTranslationSource,
        translationSources = translationSources,
        modifier = modifier,
    )
}

@Composable
internal fun SurahReaderScreen(
    uiState: ReaderUiState,
    onBackClick: () -> Unit,
    onAyahVisible: (Int) -> Unit,
    onReaderModeChange: (ReaderMode) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onTranslationSourceChange: (String) -> Unit,
    translationSources: List<TranslationSource>,
    modifier: Modifier = Modifier,
) {
    ReportDrawnWhen { uiState !is ReaderUiState.Loading }
    TrackScreenViewEvent(screenName = "SurahReader")

    var showReaderSettings by rememberSaveable { mutableStateOf(false) }

    when (uiState) {
        ReaderUiState.Loading -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .testTag("reader:loading"),
                contentAlignment = Alignment.Center,
            ) {
                SurauLoadingWheel(contentDesc = stringResource(R.string.feature_quran_impl_loading))
            }
        }

        ReaderUiState.Error -> Column(modifier = modifier.fillMaxSize()) {
            ReaderTopBar(title = "", subtitle = null, onBackClick = onBackClick, onSettingsClick = {})
            QuranErrorContent(modifier = Modifier.fillMaxSize())
        }

        is ReaderUiState.Success -> {
            val content = uiState.content
            val listState = rememberLazyListState()

            // Resume/scroll-to-ayah once per surah load.
            LaunchedEffect(content.surah?.surahId, uiState.initialAyahNumber) {
                val target = uiState.initialAyahNumber ?: return@LaunchedEffect
                val index = content.ayahs.indexOfFirst { it.ayah.ayahNumber == target }
                if (index >= 0) listState.scrollToItem(index)
            }

            // Report the most-visible (first fully visible) ayah for progress tracking.
            LaunchedEffect(listState, content.ayahs) {
                snapshotFlow {
                    listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index
                }
                    .distinctUntilChanged()
                    .collect { index ->
                        if (index != null) {
                            content.ayahs.getOrNull(index)?.let { onAyahVisible(it.ayah.ayahNumber) }
                        }
                    }
            }

            val visibleAyah by remember(content.ayahs) {
                derivedVisibleAyah(listState, content.ayahs)
            }

            Column(modifier = modifier.fillMaxSize()) {
                ReaderTopBar(
                    title = content.surah?.nameLatin.orEmpty(),
                    subtitle = visibleAyah?.let { ayah ->
                        listOfNotNull(
                            ayah.juzNumber?.let {
                                stringResource(R.string.feature_quran_impl_juz_number, it)
                            },
                            ayah.pageNumber?.let {
                                stringResource(R.string.feature_quran_impl_page_indicator, it)
                            },
                        ).joinToString(" • ").ifEmpty { null }
                    },
                    onBackClick = onBackClick,
                    onSettingsClick = { showReaderSettings = true },
                )

                TrackScrollJank(scrollableState = listState, stateName = "reader:ayahs")

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("reader:ayahList"),
                ) {
                    items(content.ayahs, key = { it.ayah.ayahNumber }) { populated ->
                        AyahItem(
                            populated = populated,
                            readerMode = content.readerMode,
                            fontScale = content.arabicFontScale,
                        )
                    }
                }
            }

            if (showReaderSettings) {
                ReaderSettingsSheet(
                    readerMode = content.readerMode,
                    fontScale = content.arabicFontScale,
                    translationSourceId = content.translationSourceId,
                    translationSources = translationSources,
                    onReaderModeChange = onReaderModeChange,
                    onFontScaleChange = onFontScaleChange,
                    onTranslationSourceChange = onTranslationSourceChange,
                    onDismiss = { showReaderSettings = false },
                )
            }
        }
    }
}

private fun derivedVisibleAyah(
    listState: LazyListState,
    ayahs: List<PopulatedAyah>,
) = androidx.compose.runtime.derivedStateOf {
    listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index
        ?.let { ayahs.getOrNull(it)?.ayah }
}

@Composable
private fun ReaderTopBar(
    title: String,
    subtitle: String?,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClick, modifier = Modifier.testTag("reader:back")) {
            Icon(
                imageVector = SurauIcons.ArrowBack,
                contentDescription = stringResource(R.string.feature_quran_impl_back),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onSettingsClick, modifier = Modifier.testTag("reader:settings")) {
            Icon(
                imageVector = SurauIcons.Settings,
                contentDescription = stringResource(R.string.feature_quran_impl_reader_settings),
            )
        }
    }
}

@Composable
private fun AyahItem(
    populated: PopulatedAyah,
    readerMode: ReaderMode,
    fontScale: Float,
) {
    val context = LocalContext.current

    val shareText = buildString {
        append(populated.ayah.textQpcHafs)
        populated.translation?.let { append("\n\n").append(it.text) }
        append("\n(QS ").append(populated.ayah.ayahKey.value).append(")")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, null))
                },
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("reader:ayah:${populated.ayah.ayahNumber}"),
    ) {
        AyahNumberBadge(populated.ayah.ayahNumber)

        if (readerMode != ReaderMode.TRANSLATION_ONLY) {
            AyahText(
                text = populated.ayah.textQpcHafs,
                fontScale = fontScale,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (readerMode != ReaderMode.ARABIC_ONLY) {
            populated.translation?.let { translation ->
                Text(
                    text = translation.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun AyahNumberBadge(ayahNumber: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = ayahNumber.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun ReaderSettingsSheet(
    readerMode: ReaderMode,
    fontScale: Float,
    translationSourceId: String,
    translationSources: List<TranslationSource>,
    onReaderModeChange: (ReaderMode) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onTranslationSourceChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.feature_quran_impl_reader_mode),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.size(12.dp))
            val modes = listOf(
                ReaderMode.ARABIC_TRANSLATION to
                    stringResource(R.string.feature_quran_impl_reader_mode_arabic_translation),
                ReaderMode.TRANSLATION_ONLY to
                    stringResource(R.string.feature_quran_impl_reader_mode_translation),
                ReaderMode.ARABIC_ONLY to
                    stringResource(R.string.feature_quran_impl_reader_mode_arabic),
            )
            SurauButtonGroup(
                options = modes.map { it.second },
                selectedIndex = modes.indexOfFirst { it.first == readerMode }.coerceAtLeast(0),
                onOptionSelected = { index -> onReaderModeChange(modes[index].first) },
            )

            Spacer(modifier = Modifier.size(24.dp))
            Text(
                text = stringResource(R.string.feature_quran_impl_arabic_font_scale),
                style = MaterialTheme.typography.titleMedium,
            )
            Slider(
                value = fontScale,
                onValueChange = onFontScaleChange,
                valueRange = 0.8f..1.8f,
                steps = 4,
            )
            AyahText(
                text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
                fontScale = fontScale,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (translationSources.isNotEmpty()) {
                Spacer(modifier = Modifier.size(24.dp))
                Text(
                    text = stringResource(R.string.feature_quran_impl_translation_source),
                    style = MaterialTheme.typography.titleMedium,
                )
                translationSources.forEach { source ->
                    ListItem(
                        headlineContent = { Text(source.name) },
                        supportingContent = source.translator?.let { { Text(it) } },
                        leadingContent = {
                            RadioButton(
                                selected = source.id == translationSourceId,
                                onClick = null,
                            )
                        },
                        modifier = Modifier.combinedClickable(
                            onClick = { onTranslationSourceChange(source.id) },
                            onLongClick = null,
                        ),
                    )
                }
            }
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}
