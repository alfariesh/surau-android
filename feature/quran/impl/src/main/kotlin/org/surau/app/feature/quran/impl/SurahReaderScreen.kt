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

import android.widget.Toast
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import org.surau.app.core.designsystem.component.AyahText
import org.surau.app.core.designsystem.component.SurauButtonGroup
import org.surau.app.core.designsystem.component.SurauDropdownMenu
import org.surau.app.core.designsystem.component.SurauLoadingWheel
import org.surau.app.core.designsystem.component.SurauMenuItem
import org.surau.app.core.designsystem.component.SurauSurfaceVariant
import org.surau.app.core.designsystem.component.SurauSwitch
import org.surau.app.core.designsystem.component.SurauWidget
import org.surau.app.core.designsystem.icon.SurauIcons
import org.surau.app.core.designsystem.theme.SurauTheme
import org.surau.app.core.media.PlayerUiState
import org.surau.app.core.model.data.quran.Bookmark
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
    val bookmarksByAyah by viewModel.bookmarksByAyah.collectAsStateWithLifecycle()

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
        bookmarksByAyah = bookmarksByAyah,
        onToggleBookmark = viewModel::toggleBookmark,
        onSaveBookmark = viewModel::saveBookmark,
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
    bookmarksByAyah: Map<Int, Bookmark> = emptyMap(),
    onToggleBookmark: (Int) -> Unit = {},
    onSaveBookmark: (ayahNumber: Int, note: String?, tags: List<String>) -> Unit = { _, _, _ -> },
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
            // Auto-scroll the reader to the actively reciting ayah (the global player now owns the
            // transport controls that used to live in an inline bar here).
            val followRecitation = true

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
                            val bookmark = bookmarksByAyah[ayahNumber]
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
                                isBookmarked = bookmark != null,
                                surahName = surah?.nameLatin.orEmpty(),
                                bookmarkNote = bookmark?.note,
                                bookmarkTags = bookmark?.tags ?: emptyList(),
                                onPlayClick = {
                                    if (isActive) onPlayPause() else onPlayAyah(ayahNumber)
                                },
                                onToggleBookmark = { onToggleBookmark(ayahNumber) },
                                onSaveBookmark = { note, tags ->
                                    onSaveBookmark(ayahNumber, note, tags)
                                },
                            )
                        }
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
    surahName: String,
    bookmarkNote: String?,
    bookmarkTags: List<String>,
    onPlayClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    onSaveBookmark: (note: String?, tags: List<String>) -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val copiedMessage = stringResource(R.string.feature_quran_impl_copied)
    var menuExpanded by remember { mutableStateOf(false) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    val ayahNumber = populated.ayah.ayahNumber

    val copyText = buildString {
        append(populated.ayah.textQpcHafs)
        populated.translation?.let { append("\n\n").append(it.text) }
        append("\n(QS ").append(populated.ayah.ayahKey.value).append(")")
    }

    val showArabic = readerMode != ReaderMode.TRANSLATION_ONLY
    val translationVisible = readerMode == ReaderMode.TRANSLATION_ONLY ||
        (readerMode != ReaderMode.ARABIC_ONLY && showTranslation)
    val transliterationVisible =
        showArabic && showTransliteration && populated.transliteration != null
    val hasBody = (translationVisible && populated.translation != null) || transliterationVisible

    // The currently-playing ayah gets an accent outline around its card.
    val activeBorder by animateColorAsState(
        targetValue = if (isActive) SurauTheme.colors.accent else Color.Transparent,
        label = "ayahActiveBorder",
    )
    val shape = RoundedCornerShape(16.dp)
    val cardModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .border(width = 1.5.dp, color = activeBorder, shape = shape)
        .testTag("reader:ayah:$ayahNumber")

    // The heading band (ayah + kebab actions menu) is shared by both card layouts.
    val heading: @Composable () -> Unit = {
        AyahHeading(
            populated = populated,
            ayahNumber = ayahNumber,
            showArabic = showArabic,
            fontScale = fontScale,
            arabicLineSpacing = arabicLineSpacing,
            isPlaying = isPlaying,
            isBookmarked = isBookmarked,
            menuExpanded = menuExpanded,
            onMenuExpandedChange = { menuExpanded = it },
            onPlay = onPlayClick,
            onToggleBookmark = onToggleBookmark,
            onCopy = {
                clipboardManager.setText(AnnotatedString(copyText))
                Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
            },
            onAddNote = { showEditor = true },
        )
    }

    if (hasBody) {
        SurauWidget(
            modifier = cardModifier,
            shellColor = SurauTheme.colors.surface,
            containerVariant = SurauSurfaceVariant.Background,
            title = heading,
            content = {
                if (transliterationVisible) {
                    populated.transliteration?.let { transliteration ->
                        Text(
                            text = transliteration.text,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize *
                                    translationScale,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (translationVisible) {
                    populated.translation?.let { translation ->
                        Text(
                            text = translation.text,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize *
                                    translationScale,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = if (transliterationVisible) {
                                Modifier.padding(top = 8.dp)
                            } else {
                                Modifier
                            },
                        )
                    }
                }
            },
        )
    } else {
        // Arabic-only with nothing to show beneath it: a single surface card, no inner body.
        Column(
            modifier = cardModifier
                .clip(shape)
                .background(SurauTheme.colors.surface)
                .padding(6.dp),
        ) {
            heading()
        }
    }

    if (showEditor) {
        ModalBottomSheet(
            onDismissRequest = { showEditor = false },
            modifier = Modifier.testTag("reader:bookmarkEditor"),
        ) {
            BookmarkEditorContent(
                item = BookmarkListItem(
                    ayahKey = populated.ayah.ayahKey,
                    surahId = populated.ayah.surahId,
                    ayahNumber = populated.ayah.ayahNumber,
                    surahName = surahName,
                    arabicText = populated.ayah.textQpcHafs,
                    note = bookmarkNote,
                    tags = bookmarkTags,
                ),
                onSave = { note, tags ->
                    onSaveBookmark(note, tags)
                    showEditor = false
                },
            )
        }
    }
}

/**
 * The widget's heading band: a kebab actions menu on the left and the Arabic ayah filling the rest
 * (right-aligned, RTL). When Arabic is hidden (translation-only) the ayah number takes its place so
 * the band still identifies the verse.
 */
@Composable
private fun AyahHeading(
    populated: PopulatedAyah,
    ayahNumber: Int,
    showArabic: Boolean,
    fontScale: Float,
    arabicLineSpacing: Float,
    isPlaying: Boolean,
    isBookmarked: Boolean,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    onPlay: () -> Unit,
    onToggleBookmark: () -> Unit,
    onCopy: () -> Unit,
    onAddNote: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            IconButton(
                onClick = { onMenuExpandedChange(true) },
                modifier = Modifier.testTag("reader:menu:$ayahNumber"),
            ) {
                Icon(
                    imageVector = SurauIcons.MoreVert,
                    contentDescription = stringResource(R.string.feature_quran_impl_ayah_actions),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AyahActionsMenu(
                expanded = menuExpanded,
                ayahNumber = ayahNumber,
                isPlaying = isPlaying,
                isBookmarked = isBookmarked,
                onDismiss = { onMenuExpandedChange(false) },
                onPlay = {
                    onMenuExpandedChange(false)
                    onPlay()
                },
                onToggleBookmark = {
                    onMenuExpandedChange(false)
                    onToggleBookmark()
                },
                onCopy = {
                    onMenuExpandedChange(false)
                    onCopy()
                },
                onAddNote = {
                    onMenuExpandedChange(false)
                    onAddNote()
                },
            )
        }
        if (showArabic) {
            AyahText(
                text = populated.ayah.textQpcHafs,
                fontScale = fontScale,
                lineHeightMultiplier = arabicLineSpacing,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            )
        } else {
            Text(
                text = stringResource(R.string.feature_quran_impl_ayah_number, ayahNumber),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            )
        }
    }
}

/** The kebab dropdown: play this ayah, save (bookmark), copy, and add notes. */
@Composable
private fun AyahActionsMenu(
    expanded: Boolean,
    ayahNumber: Int,
    isPlaying: Boolean,
    isBookmarked: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onToggleBookmark: () -> Unit,
    onCopy: () -> Unit,
    onAddNote: () -> Unit,
) {
    SurauDropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        SurauMenuItem(
            text = stringResource(
                if (isPlaying) R.string.feature_quran_impl_pause else R.string.feature_quran_impl_play,
            ),
            onClick = onPlay,
            leading = {
                Icon(
                    imageVector = if (isPlaying) SurauIcons.Pause else SurauIcons.PlayArrow,
                    contentDescription = null,
                )
            },
        )
        SurauMenuItem(
            text = stringResource(
                if (isBookmarked) {
                    R.string.feature_quran_impl_unbookmark
                } else {
                    R.string.feature_quran_impl_bookmark
                },
            ),
            onClick = onToggleBookmark,
            modifier = Modifier.testTag("reader:menu:save:$ayahNumber"),
            leading = {
                Icon(
                    imageVector = if (isBookmarked) SurauIcons.Bookmark else SurauIcons.BookmarkBorder,
                    contentDescription = null,
                )
            },
        )
        SurauMenuItem(
            text = stringResource(R.string.feature_quran_impl_copy),
            onClick = onCopy,
            leading = { Icon(SurauIcons.ContentCopy, contentDescription = null) },
        )
        SurauMenuItem(
            text = stringResource(R.string.feature_quran_impl_bookmark_note),
            onClick = onAddNote,
            modifier = Modifier.testTag("reader:menu:note:$ayahNumber"),
            leading = { Icon(SurauIcons.ShortText, contentDescription = null) },
        )
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
            // Transliteration renders under the Arabic line, so it's only meaningful when Arabic is
            // shown. Hide the toggle in translation-only mode rather than leaving a dead control.
            if (readerMode != ReaderMode.TRANSLATION_ONLY) {
                ReaderSwitchRow(
                    label = stringResource(R.string.feature_quran_impl_reader_transliteration),
                    checked = showTransliteration,
                    onCheckedChange = onShowTransliterationChange,
                )
            }
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
