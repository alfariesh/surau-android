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
import android.widget.Toast
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.surau.app.core.designsystem.component.AyahText
import org.surau.app.core.designsystem.component.SurauButtonGroup
import org.surau.app.core.designsystem.component.SurauLoadingWheel
import org.surau.app.core.designsystem.component.SurauSwitch
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
    onFlowClick: (Int?) -> Unit = {},
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
    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
    val bookmarkedAyahNumbers by viewModel.bookmarkedAyahNumbers.collectAsStateWithLifecycle()

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
        onFlowClick = onFlowClick,
        keepScreenOn = keepScreenOn,
        onShowTransliterationChange = viewModel::setShowTransliteration,
        onShowTranslationChange = viewModel::setShowTranslation,
        onArabicLineSpacingChange = viewModel::setArabicLineSpacing,
        onTranslationScaleChange = viewModel::setTranslationScale,
        onKeepScreenOnChange = viewModel::setKeepScreenOn,
        bookmarkedAyahNumbers = bookmarkedAyahNumbers,
        onToggleBookmark = viewModel::toggleBookmark,
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
    onFlowClick: (Int?) -> Unit = {},
    keepScreenOn: Boolean = false,
    onShowTransliterationChange: (Boolean) -> Unit = {},
    onShowTranslationChange: (Boolean) -> Unit = {},
    onArabicLineSpacingChange: (Float) -> Unit = {},
    onTranslationScaleChange: (Float) -> Unit = {},
    onKeepScreenOnChange: (Boolean) -> Unit = {},
    bookmarkedAyahNumbers: Set<Int> = emptySet(),
    onToggleBookmark: (Int) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    ReportDrawnWhen { uiState !is ReaderUiState.Loading }
    TrackScreenViewEvent(screenName = "SurahReader")

    // Keep the screen awake while reading when the user has opted in.
    val view = LocalView.current
    DisposableEffect(keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

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
            ReaderTopBar(
                title = "",
                subtitle = null,
                onBackClick = onBackClick,
                onSettingsClick = {},
                onFlowClick = {},
            )
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
                        onFlowClick = {
                            onFlowClick(
                                playerState.currentAyahNumber
                                    ?.takeIf { playerState.surahId == surah?.surahId }
                                    ?: visibleAyah?.ayahNumber,
                            )
                        },
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
                                showTransliteration = content.showTransliteration,
                                showTranslation = content.showTranslation,
                                arabicLineSpacing = content.arabicLineSpacing,
                                translationScale = content.translationScale,
                                isActive = isActive,
                                isPlaying = isActive && playerState.isPlaying,
                                isBookmarked = ayahNumber in bookmarkedAyahNumbers,
                                onPlayClick = {
                                    if (isActive) onPlayPause() else onPlayAyah(ayahNumber)
                                },
                                onToggleBookmark = { onToggleBookmark(ayahNumber) },
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
                    showTransliteration = content.showTransliteration,
                    showTranslation = content.showTranslation,
                    arabicLineSpacing = content.arabicLineSpacing,
                    translationScale = content.translationScale,
                    keepScreenOn = keepScreenOn,
                    onReaderModeChange = onReaderModeChange,
                    onFontScaleChange = onFontScaleChange,
                    onTranslationSourceChange = onTranslationSourceChange,
                    onRecitationChange = onRecitationChange,
                    onShowTransliterationChange = onShowTransliterationChange,
                    onShowTranslationChange = onShowTranslationChange,
                    onArabicLineSpacingChange = onArabicLineSpacingChange,
                    onTranslationScaleChange = onTranslationScaleChange,
                    onKeepScreenOnChange = onKeepScreenOnChange,
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
    onFlowClick: () -> Unit,
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
            AssistChip(
                onClick = onFlowClick,
                label = { Text(stringResource(R.string.feature_quran_impl_flow)) },
                leadingIcon = {
                    Icon(
                        imageVector = SurauIcons.Flow,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                },
                modifier = Modifier
                    .padding(top = 4.dp)
                    .testTag("reader:flow"),
            )
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
    showTransliteration: Boolean,
    showTranslation: Boolean,
    arabicLineSpacing: Float,
    translationScale: Float,
    isActive: Boolean,
    isPlaying: Boolean,
    isBookmarked: Boolean,
    onPlayClick: () -> Unit,
    onToggleBookmark: () -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val copiedMessage = stringResource(R.string.feature_quran_impl_copied)
    var showActions by rememberSaveable { mutableStateOf(false) }

    val shareText = buildString {
        append(populated.ayah.textQpcHafs)
        populated.translation?.let { append("\n\n").append(it.text) }
        append("\n(QS ").append(populated.ayah.ayahKey.value).append(")")
    }

    fun share() {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(sendIntent, null))
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
                onLongClick = { showActions = true },
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
            IconToggleButton(
                checked = isBookmarked,
                onCheckedChange = { onToggleBookmark() },
                modifier = Modifier.testTag("reader:bookmark:${populated.ayah.ayahNumber}"),
            ) {
                Icon(
                    imageVector = if (isBookmarked) {
                        SurauIcons.Bookmark
                    } else {
                        SurauIcons.BookmarkBorder
                    },
                    contentDescription = stringResource(
                        if (isBookmarked) {
                            R.string.feature_quran_impl_unbookmark
                        } else {
                            R.string.feature_quran_impl_bookmark
                        },
                    ),
                    tint = if (isBookmarked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
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
                lineHeightMultiplier = arabicLineSpacing,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp),
            )

            if (showTransliteration) {
                populated.transliteration?.let { transliteration ->
                    Text(
                        text = transliteration.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = FontStyle.Italic,
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize * translationScale,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        // The translation is shown when the mode allows it AND the user hasn't hidden it; in
        // translation-only mode it is always shown (it's the only content).
        val translationVisible = readerMode == ReaderMode.TRANSLATION_ONLY ||
            (readerMode != ReaderMode.ARABIC_ONLY && showTranslation)
        if (translationVisible) {
            populated.translation?.let { translation ->
                Text(
                    text = translation.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize * translationScale,
                    ),
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

    if (showActions) {
        ModalBottomSheet(
            onDismissRequest = { showActions = false },
            modifier = Modifier.testTag("reader:ayahActions"),
        ) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.feature_quran_impl_copy)) },
                    leadingContent = { Icon(SurauIcons.ContentCopy, contentDescription = null) },
                    modifier = Modifier.clickable {
                        clipboardManager.setText(AnnotatedString(shareText))
                        Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                        showActions = false
                    },
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.feature_quran_impl_share)) },
                    leadingContent = { Icon(SurauIcons.Share, contentDescription = null) },
                    modifier = Modifier.clickable {
                        share()
                        showActions = false
                    },
                )
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(
                                if (isBookmarked) {
                                    R.string.feature_quran_impl_unbookmark
                                } else {
                                    R.string.feature_quran_impl_bookmark
                                },
                            ),
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = if (isBookmarked) {
                                SurauIcons.Bookmark
                            } else {
                                SurauIcons.BookmarkBorder
                            },
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier
                        .testTag("reader:ayahActions:bookmark")
                        .clickable {
                            onToggleBookmark()
                            showActions = false
                        },
                )
            }
        }
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
    showTransliteration: Boolean,
    showTranslation: Boolean,
    arabicLineSpacing: Float,
    translationScale: Float,
    keepScreenOn: Boolean,
    onReaderModeChange: (ReaderMode) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onTranslationSourceChange: (String) -> Unit,
    onRecitationChange: (String) -> Unit,
    onShowTransliterationChange: (Boolean) -> Unit,
    onShowTranslationChange: (Boolean) -> Unit,
    onArabicLineSpacingChange: (Float) -> Unit,
    onTranslationScaleChange: (Float) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
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
                lineHeightMultiplier = arabicLineSpacing,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.size(24.dp))
            Text(
                text = stringResource(R.string.feature_quran_impl_reader_line_spacing),
                style = MaterialTheme.typography.titleMedium,
            )
            Slider(
                value = arabicLineSpacing,
                onValueChange = onArabicLineSpacingChange,
                valueRange = 1f..2f,
                steps = 3,
            )

            Spacer(modifier = Modifier.size(24.dp))
            Text(
                text = stringResource(R.string.feature_quran_impl_reader_display),
                style = MaterialTheme.typography.titleMedium,
            )
            ReaderSwitchRow(
                label = stringResource(R.string.feature_quran_impl_reader_transliteration),
                checked = showTransliteration,
                onCheckedChange = onShowTransliterationChange,
            )
            ReaderSwitchRow(
                label = stringResource(R.string.feature_quran_impl_reader_show_translation),
                checked = showTranslation,
                onCheckedChange = onShowTranslationChange,
            )
            ReaderSwitchRow(
                label = stringResource(R.string.feature_quran_impl_reader_keep_screen_on),
                checked = keepScreenOn,
                onCheckedChange = onKeepScreenOnChange,
            )

            Spacer(modifier = Modifier.size(24.dp))
            Text(
                text = stringResource(R.string.feature_quran_impl_reader_translation_size),
                style = MaterialTheme.typography.titleMedium,
            )
            Slider(
                value = translationScale,
                onValueChange = onTranslationScaleChange,
                valueRange = 0.8f..1.6f,
                steps = 3,
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

@Composable
private fun ReaderSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        SurauSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
