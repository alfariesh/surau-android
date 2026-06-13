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
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.surau.app.core.designsystem.component.AyahText
import org.surau.app.core.designsystem.component.SurauButtonGroup
import org.surau.app.core.designsystem.component.SurauLoadingWheel
import org.surau.app.core.designsystem.icon.SurauIcons
import org.surau.app.core.media.PlayerUiState
import org.surau.app.core.model.data.quran.PopulatedAyah
import org.surau.app.core.model.data.quran.ReaderMode
import org.surau.app.core.model.data.quran.Recitation
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
    val recitations by viewModel.recitations.collectAsStateWithLifecycle()
    val selectedRecitationId by viewModel.selectedRecitationId.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val playingAyah by viewModel.playingAyah.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val offlineMessage = stringResource(R.string.feature_quran_impl_audio_offline)
    val partialMessage = stringResource(R.string.feature_quran_impl_audio_partial)
    LaunchedEffect(Unit) {
        viewModel.audioError.collect { snackbarHostState.showSnackbar(offlineMessage) }
    }
    LaunchedEffect(Unit) {
        viewModel.audioPartial.collect { snackbarHostState.showSnackbar(partialMessage) }
    }

    SurahReaderScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onAyahVisible = viewModel::onAyahVisible,
        onReaderModeChange = viewModel::setReaderMode,
        onFontScaleChange = viewModel::setArabicFontScale,
        onTranslationSourceChange = viewModel::setTranslationSource,
        translationSources = translationSources,
        playingAyah = playingAyah,
        playerState = playerState,
        recitations = recitations,
        selectedRecitationId = selectedRecitationId,
        onPlayAyah = viewModel::playFromAyah,
        onPlayPause = viewModel::onPlayPause,
        onNext = viewModel::onNext,
        onPrevious = viewModel::onPrevious,
        onRecitationChange = viewModel::setRecitation,
        snackbarHostState = snackbarHostState,
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
    playingAyah: Int? = null,
    playerState: PlayerUiState = PlayerUiState(),
    recitations: List<Recitation> = emptyList(),
    selectedRecitationId: String? = null,
    onPlayAyah: (Int) -> Unit = {},
    onPlayPause: () -> Unit = {},
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onRecitationChange: (String) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
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
            val surah = content.surah
            val listState = rememberLazyListState()
            val scope = rememberCoroutineScope()
            var followRecitation by rememberSaveable { mutableStateOf(true) }

            // Resume/scroll-to-ayah once per surah load.
            LaunchedEffect(surah?.surahId, uiState.initialAyahNumber) {
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

            // Auto-scroll to the playing ayah while "ikuti bacaan" is on.
            LaunchedEffect(playingAyah, followRecitation, content.ayahs) {
                if (followRecitation && playingAyah != null) {
                    val index = content.ayahs.indexOfFirst { it.ayah.ayahNumber == playingAyah }
                    if (index >= 0) listState.animateScrollToItem(index)
                }
            }

            val visibleAyah by remember(content.ayahs) {
                derivedVisibleAyah(listState, content.ayahs)
            }

            Box(modifier = modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    ReaderTopBar(
                        title = surah?.nameLatin.orEmpty(),
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
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("reader:ayahList"),
                    ) {
                        items(content.ayahs, key = { it.ayah.ayahNumber }) { populated ->
                            val ayahNumber = populated.ayah.ayahNumber
                            val isActive = ayahNumber == playingAyah
                            AyahItem(
                                populated = populated,
                                readerMode = content.readerMode,
                                fontScale = content.arabicFontScale,
                                isActive = isActive,
                                isPlaying = isActive && playerState.isPlaying,
                                onPlayClick = {
                                    if (isActive) onPlayPause() else onPlayAyah(ayahNumber)
                                },
                            )
                        }
                    }

                    if (surah != null && playerState.surahId == surah.surahId) {
                        MiniPlayerBar(
                            surahName = surah.nameLatin,
                            playerState = playerState,
                            followRecitation = followRecitation,
                            onPlayPause = onPlayPause,
                            onPrevious = onPrevious,
                            onNext = onNext,
                            onToggleFollow = { followRecitation = it },
                            onBarClick = {
                                val target = playerState.currentAyahNumber
                                if (target != null) {
                                    val index =
                                        content.ayahs.indexOfFirst { it.ayah.ayahNumber == target }
                                    if (index >= 0) {
                                        scope.launch { listState.animateScrollToItem(index) }
                                    }
                                }
                            },
                        )
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            if (showReaderSettings) {
                ReaderSettingsSheet(
                    readerMode = content.readerMode,
                    fontScale = content.arabicFontScale,
                    translationSourceId = content.translationSourceId,
                    translationSources = translationSources,
                    recitations = recitations,
                    selectedRecitationId = selectedRecitationId,
                    onReaderModeChange = onReaderModeChange,
                    onFontScaleChange = onFontScaleChange,
                    onTranslationSourceChange = onTranslationSourceChange,
                    onRecitationChange = onRecitationChange,
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
    isActive: Boolean,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
) {
    val context = LocalContext.current

    val shareText = buildString {
        append(populated.ayah.textQpcHafs)
        populated.translation?.let { append("\n\n").append(it.text) }
        append("\n(QS ").append(populated.ayah.ayahKey.value).append(")")
    }

    val highlightColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        } else {
            Color.Transparent
        },
        label = "ayahHighlight",
    )

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
            .background(highlightColor)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("reader:ayah:${populated.ayah.ayahNumber}"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AyahNumberBadge(populated.ayah.ayahNumber)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = onPlayClick,
                modifier = Modifier.testTag("reader:play:${populated.ayah.ayahNumber}"),
            ) {
                Icon(
                    imageVector = if (isPlaying) SurauIcons.Pause else SurauIcons.PlayArrow,
                    contentDescription = stringResource(
                        if (isPlaying) {
                            R.string.feature_quran_impl_pause
                        } else {
                            R.string.feature_quran_impl_play
                        },
                    ),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

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
private fun MiniPlayerBar(
    surahName: String,
    playerState: PlayerUiState,
    followRecitation: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleFollow: (Boolean) -> Unit,
    onBarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("reader:miniPlayer"),
    ) {
        Column {
            val progress = if (playerState.durationMs > 0L) {
                (playerState.positionMs.toFloat() / playerState.durationMs).coerceIn(0f, 1f)
            } else {
                0f
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        R.string.feature_quran_impl_now_playing,
                        surahName,
                        playerState.currentAyahNumber ?: 0,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onBarClick)
                        .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
                )
                IconToggleButton(checked = followRecitation, onCheckedChange = onToggleFollow) {
                    Icon(
                        imageVector = if (followRecitation) {
                            SurauIcons.FollowReading
                        } else {
                            SurauIcons.FollowReadingOff
                        },
                        contentDescription = stringResource(
                            R.string.feature_quran_impl_follow_recitation,
                        ),
                        tint = if (followRecitation) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                IconButton(onClick = onPrevious) {
                    Icon(
                        imageVector = SurauIcons.SkipPrevious,
                        contentDescription = stringResource(
                            R.string.feature_quran_impl_previous_ayah,
                        ),
                    )
                }
                FilledTonalIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.testTag("reader:miniPlayer:playPause"),
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) {
                            SurauIcons.Pause
                        } else {
                            SurauIcons.PlayArrow
                        },
                        contentDescription = stringResource(
                            if (playerState.isPlaying) {
                                R.string.feature_quran_impl_pause
                            } else {
                                R.string.feature_quran_impl_play
                            },
                        ),
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.padding(end = 4.dp)) {
                    Icon(
                        imageVector = SurauIcons.SkipNext,
                        contentDescription = stringResource(R.string.feature_quran_impl_next_ayah),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderSettingsSheet(
    readerMode: ReaderMode,
    fontScale: Float,
    translationSourceId: String,
    translationSources: List<TranslationSource>,
    recitations: List<Recitation>,
    selectedRecitationId: String?,
    onReaderModeChange: (ReaderMode) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onTranslationSourceChange: (String) -> Unit,
    onRecitationChange: (String) -> Unit,
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

            if (recitations.isNotEmpty()) {
                Spacer(modifier = Modifier.size(24.dp))
                Text(
                    text = stringResource(R.string.feature_quran_impl_qari),
                    style = MaterialTheme.typography.titleMedium,
                )
                val selected = selectedRecitationId
                    ?: recitations.firstOrNull { it.isDefault }?.id
                recitations.forEach { recitation ->
                    ListItem(
                        headlineContent = { Text(recitation.displayName) },
                        supportingContent = recitation.style?.let { { Text(it) } },
                        leadingContent = {
                            RadioButton(
                                selected = recitation.id == selected,
                                onClick = null,
                            )
                        },
                        modifier = Modifier.combinedClickable(
                            onClick = { onRecitationChange(recitation.id) },
                            onLongClick = null,
                        ),
                    )
                }
            }
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}
