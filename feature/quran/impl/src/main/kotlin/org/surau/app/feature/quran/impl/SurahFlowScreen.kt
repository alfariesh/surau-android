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

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package org.surau.app.feature.quran.impl

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.surau.app.core.designsystem.component.AyahText
import org.surau.app.core.designsystem.component.SurauLoadingWheel
import org.surau.app.core.designsystem.icon.SurauIcons
import org.surau.app.core.media.PlayerUiState
import org.surau.app.core.model.data.quran.PopulatedAyah
import org.surau.app.core.ui.TrackScreenViewEvent
import org.surau.app.feature.quran.api.navigation.SurahFlowNavKey

@Composable
fun SurahFlowScreen(
    navKey: SurahFlowNavKey,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SurahFlowViewModel =
        hiltViewModel<SurahFlowViewModel, SurahFlowViewModel.Factory>(
            key = navKey.toString(),
        ) { factory ->
            factory.create(navKey)
        },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val playingAyah by viewModel.playingAyah.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val showTranslation by viewModel.showTranslation.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val offlineMessage = stringResource(R.string.feature_quran_impl_audio_offline)
    LaunchedEffect(Unit) {
        viewModel.audioError.collect { snackbarHostState.showSnackbar(offlineMessage) }
    }

    SurahFlowScreen(
        uiState = uiState,
        playingAyah = playingAyah,
        playerState = playerState,
        fontScale = fontScale,
        showTranslation = showTranslation,
        onBackClick = onBackClick,
        onPlayPause = viewModel::onPlayPause,
        onNext = viewModel::onNext,
        onPrevious = viewModel::onPrevious,
        onSeekToAyah = viewModel::onSeekToAyah,
        onSetFontScale = viewModel::setFontScale,
        onToggleTranslation = viewModel::toggleTranslation,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
internal fun SurahFlowScreen(
    uiState: FlowUiState,
    playingAyah: Int?,
    playerState: PlayerUiState,
    fontScale: Float,
    showTranslation: Boolean,
    onBackClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekToAyah: (Int) -> Unit,
    onSetFontScale: (Float) -> Unit,
    onToggleTranslation: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    initialChromeVisible: Boolean = true,
) {
    ReportDrawnWhen { uiState !is FlowUiState.Loading }
    TrackScreenViewEvent(screenName = "SurahFlow")

    when (uiState) {
        FlowUiState.Loading -> Box(
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .testTag("flow:loading"),
            contentAlignment = Alignment.Center,
        ) {
            SurauLoadingWheel(contentDesc = stringResource(R.string.feature_quran_impl_loading))
        }

        FlowUiState.Error -> Column(
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.testTag("flow:back")) {
                Icon(
                    imageVector = SurauIcons.ArrowBack,
                    contentDescription = stringResource(R.string.feature_quran_impl_back),
                )
            }
            QuranErrorContent(modifier = Modifier.fillMaxSize())
        }

        is FlowUiState.Success -> FlowSuccess(
            success = uiState,
            playingAyah = playingAyah,
            playerState = playerState,
            fontScale = fontScale,
            showTranslation = showTranslation,
            onBackClick = onBackClick,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onPrevious = onPrevious,
            onSeekToAyah = onSeekToAyah,
            onSetFontScale = onSetFontScale,
            onToggleTranslation = onToggleTranslation,
            snackbarHostState = snackbarHostState,
            initialChromeVisible = initialChromeVisible,
            modifier = modifier,
        )
    }
}

@Composable
private fun FlowSuccess(
    success: FlowUiState.Success,
    playingAyah: Int?,
    playerState: PlayerUiState,
    fontScale: Float,
    showTranslation: Boolean,
    onBackClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekToAyah: (Int) -> Unit,
    onSetFontScale: (Float) -> Unit,
    onToggleTranslation: () -> Unit,
    snackbarHostState: SnackbarHostState,
    initialChromeVisible: Boolean,
    modifier: Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface
    val listState = rememberLazyListState()
    val inspection = LocalInspectionMode.current

    var chromeVisible by rememberSaveable { mutableStateOf(initialChromeVisible) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    // Auto-hide chrome after a few idle seconds (skipped in previews/screenshots).
    LaunchedEffect(chromeVisible, interactionTick) {
        if (!inspection && chromeVisible) {
            delay(CHROME_IDLE_MS)
            chromeVisible = false
        }
    }

    // Immersive: hide the system bars while the chrome is hidden; always restore them on leave.
    val view = LocalView.current
    DisposableEffect(chromeVisible) {
        val controller = view.context.findActivity()?.window?.let {
            WindowInsetsControllerCompat(it, view)
        }
        if (chromeVisible) {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    // Keep the active ayah centred.
    LaunchedEffect(playingAyah) {
        val target = playingAyah ?: return@LaunchedEffect
        val index = success.ayahs.indexOfFirst { it.ayah.ayahNumber == target }
        if (index >= 0) listState.centerItem(index)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surface)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        chromeVisible = !chromeVisible
                        interactionTick++
                    },
                )
            }
            .testTag("flow:screen"),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // Half-viewport top/bottom padding lets any ayah scroll to the centre.
            val halfViewport = maxHeight / 2
            LazyColumn(
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = halfViewport),
                modifier = Modifier.fillMaxSize().testTag("flow:ayahList"),
            ) {
                items(success.ayahs, key = { it.ayah.ayahNumber }) { populated ->
                    FlowAyah(
                        populated = populated,
                        isActive = populated.ayah.ayahNumber == playingAyah,
                        fontScale = fontScale,
                        showTranslation = showTranslation,
                        onTap = { onSeekToAyah(populated.ayah.ayahNumber) },
                    )
                }
            }
        }

        // Fade the top and bottom edges so the centre ayah is the focus.
        Box(
            Modifier
                .fillMaxWidth()
                .height(FADE_HEIGHT)
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(surface, Color.Transparent))),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(FADE_HEIGHT)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, surface))),
        )

        AnimatedVisibility(visible = chromeVisible, modifier = Modifier.align(Alignment.TopCenter)) {
            FlowTopBar(
                surahName = success.surahName,
                onBackClick = onBackClick,
                onSettingsClick = {
                    showSettings = true
                    interactionTick++
                },
            )
        }

        AnimatedVisibility(
            visible = chromeVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            FlowBottomBar(
                playerState = playerState,
                onPlayPause = {
                    onPlayPause()
                    interactionTick++
                },
                onPrevious = {
                    onPrevious()
                    interactionTick++
                },
                onNext = {
                    onNext()
                    interactionTick++
                },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (showSettings) {
        FlowSettingsSheet(
            fontScale = fontScale,
            showTranslation = showTranslation,
            onFontScaleChange = onSetFontScale,
            onToggleTranslation = onToggleTranslation,
            onDismiss = { showSettings = false },
        )
    }
}

@Composable
private fun FlowAyah(
    populated: PopulatedAyah,
    isActive: Boolean,
    fontScale: Float,
    showTranslation: Boolean,
    onTap: () -> Unit,
) {
    val alpha by animateFloatAsState(
        targetValue = if (isActive) 1f else INACTIVE_ALPHA,
        label = "flowAyahAlpha",
    )
    val color = if (isActive) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .alpha(alpha)
            .testTag("flow:ayah:${populated.ayah.ayahNumber}"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AyahText(
            text = populated.ayah.textQpcHafs,
            fontScale = fontScale,
            color = color,
        )
        if (showTranslation) {
            populated.translation?.let { translation ->
                Text(
                    text = translation.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = color,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun FlowTopBar(
    surahName: String,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.testTag("flow:back")) {
                Icon(
                    imageVector = SurauIcons.ArrowBack,
                    contentDescription = stringResource(R.string.feature_quran_impl_back),
                )
            }
            Text(
                text = surahName,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            )
            IconButton(onClick = onSettingsClick, modifier = Modifier.testTag("flow:settings")) {
                Icon(
                    imageVector = SurauIcons.Settings,
                    contentDescription = stringResource(R.string.feature_quran_impl_flow_settings),
                )
            }
        }
    }
}

@Composable
private fun FlowBottomBar(
    playerState: PlayerUiState,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(
                        imageVector = SurauIcons.SkipPrevious,
                        contentDescription = stringResource(R.string.feature_quran_impl_previous_ayah),
                    )
                }
                FilledTonalIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .testTag("flow:playPause"),
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
                IconButton(onClick = onNext) {
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
private fun FlowSettingsSheet(
    fontScale: Float,
    showTranslation: Boolean,
    onFontScaleChange: (Float) -> Unit,
    onToggleTranslation: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.feature_quran_impl_flow_font_size),
                style = MaterialTheme.typography.titleMedium,
            )
            Slider(
                value = fontScale,
                onValueChange = onFontScaleChange,
                valueRange = 0.8f..2.4f,
                steps = 7,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.feature_quran_impl_flow_show_translation),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = showTranslation, onCheckedChange = { onToggleTranslation() })
            }
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

/** Scrolls so the centre of item [index] aligns with the viewport centre. */
private suspend fun LazyListState.centerItem(index: Int) {
    val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (item != null) {
        val delta = item.offset + item.size / 2 - layoutInfo.viewportSize.height / 2
        animateScrollBy(delta.toFloat())
    } else {
        scrollToItem(index)
        val settled = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
        val delta = settled.offset + settled.size / 2 - layoutInfo.viewportSize.height / 2
        scrollBy(delta.toFloat())
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private val FADE_HEIGHT = 120.dp
private const val CHROME_IDLE_MS = 3_000L
private const val INACTIVE_ALPHA = 0.3f
