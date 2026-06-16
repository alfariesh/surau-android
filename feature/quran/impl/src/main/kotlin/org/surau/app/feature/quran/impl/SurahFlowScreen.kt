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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.surau.app.core.designsystem.component.AyahText
import org.surau.app.core.designsystem.component.SurauLoadingWheel
import org.surau.app.core.designsystem.component.SurauMeshGradient
import org.surau.app.core.designsystem.component.SurauSwitch
import org.surau.app.core.designsystem.icon.SurauIcons
import org.surau.app.core.media.PlayerUiState
import org.surau.app.core.media.RepeatScope
import org.surau.app.core.media.SleepTimerOption
import org.surau.app.core.model.data.quran.PopulatedAyah
import org.surau.app.core.ui.TrackScreenViewEvent
import org.surau.app.feature.quran.api.navigation.SurahFlowNavKey
import kotlin.math.abs

@Composable
fun SurahFlowScreen(
    navKey: SurahFlowNavKey,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    embedded: Boolean = false,
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
    val autoContinue by viewModel.autoContinue.collectAsStateWithLifecycle()
    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()

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
        autoContinue = autoContinue,
        keepScreenOn = keepScreenOn,
        onBackClick = onBackClick,
        onPlayPause = viewModel::onPlayPause,
        onNext = viewModel::onNext,
        onPrevious = viewModel::onPrevious,
        onSeekToAyah = viewModel::onSeekToAyah,
        onSetFontScale = viewModel::setFontScale,
        onToggleTranslation = viewModel::toggleTranslation,
        onToggleAutoContinue = viewModel::toggleAutoContinue,
        onToggleKeepScreenOn = viewModel::toggleKeepScreenOn,
        onSetRepeat = viewModel::onSetRepeat,
        onSetSleepTimer = viewModel::onSetSleepTimer,
        onSetSpeed = viewModel::onSetSpeed,
        snackbarHostState = snackbarHostState,
        embedded = embedded,
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
    autoContinue: Boolean,
    keepScreenOn: Boolean,
    onBackClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekToAyah: (Int) -> Unit,
    onSetFontScale: (Float) -> Unit,
    onToggleTranslation: () -> Unit,
    onToggleAutoContinue: () -> Unit,
    onToggleKeepScreenOn: () -> Unit,
    onSetRepeat: (RepeatScope, Int) -> Unit,
    onSetSleepTimer: (SleepTimerOption) -> Unit,
    onSetSpeed: (Float) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    initialChromeVisible: Boolean = true,
    embedded: Boolean = false,
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
            autoContinue = autoContinue,
            keepScreenOn = keepScreenOn,
            onBackClick = onBackClick,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onPrevious = onPrevious,
            onSeekToAyah = onSeekToAyah,
            onSetFontScale = onSetFontScale,
            onToggleTranslation = onToggleTranslation,
            onToggleAutoContinue = onToggleAutoContinue,
            onToggleKeepScreenOn = onToggleKeepScreenOn,
            onSetRepeat = onSetRepeat,
            onSetSleepTimer = onSetSleepTimer,
            onSetSpeed = onSetSpeed,
            snackbarHostState = snackbarHostState,
            initialChromeVisible = initialChromeVisible,
            embedded = embedded,
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
    autoContinue: Boolean,
    keepScreenOn: Boolean,
    onBackClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekToAyah: (Int) -> Unit,
    onSetFontScale: (Float) -> Unit,
    onToggleTranslation: () -> Unit,
    onToggleAutoContinue: () -> Unit,
    onToggleKeepScreenOn: () -> Unit,
    onSetRepeat: (RepeatScope, Int) -> Unit,
    onSetSleepTimer: (SleepTimerOption) -> Unit,
    onSetSpeed: (Float) -> Unit,
    snackbarHostState: SnackbarHostState,
    initialChromeVisible: Boolean,
    embedded: Boolean,
    modifier: Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface
    val flowMesh = flowMeshColors()
    val listState = rememberLazyListState()
    val inspection = LocalInspectionMode.current

    var chromeVisible by rememberSaveable { mutableStateOf(initialChromeVisible) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    // Material 3 motion: a spatial spring drives the bars' slide (movement/position) while an effects
    // spring drives the fade (opacity never bounces). See m3.material.io/styles/motion.
    val chromeSlideSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val chromeFadeSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    // Auto-hide chrome after a few idle seconds (skipped in previews/screenshots).
    LaunchedEffect(chromeVisible, interactionTick) {
        if (!inspection && chromeVisible) {
            delay(CHROME_IDLE_MS)
            chromeVisible = false
        }
    }

    // Reveal the chrome when the reader drags back toward the top, hide it when reading forward —
    // the standard "enter-always" toolbar feel, so a swipe (not only a tap) summons the header.
    // Observe-only: it never consumes the delta, so the player's collapse hand-off is untouched.
    // Only user drags count; programmatic centring dispatches as SideEffect and is ignored.
    val chromeScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    val dy = available.y
                    if (dy > CHROME_SCROLL_THRESHOLD_PX) {
                        chromeVisible = true
                        interactionTick++ // keep the idle timer fresh while dragging back
                    } else if (dy < -CHROME_SCROLL_THRESHOLD_PX) {
                        chromeVisible = false
                    }
                }
                return Offset.Zero
            }
        }
    }

    // Immersive: hide the system bars while the chrome is hidden; always restore them on leave.
    // Skipped when embedded — the player is an expandable sheet that can be collapsed, so it must
    // never take over the whole window's system bars.
    val view = LocalView.current
    DisposableEffect(chromeVisible, embedded) {
        val controller = if (!embedded) {
            view.context.findActivity()?.window?.let { WindowInsetsControllerCompat(it, view) }
        } else {
            null
        }
        if (chromeVisible) {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    // Keep the screen awake while reciting (when enabled); let it sleep when paused/left/disabled.
    val awake = playerState.isPlaying && keepScreenOn
    DisposableEffect(awake) {
        view.keepScreenOn = awake
        onDispose { view.keepScreenOn = false }
    }

    // Keep the active ayah centred. Wait for the list to be measured first so centring also works
    // when entering Flow mid-playback (the active ayah is already set before the first layout pass).
    LaunchedEffect(playingAyah) {
        val target = playingAyah ?: return@LaunchedEffect
        val index = success.ayahs.indexOfFirst { it.ayah.ayahNumber == target }
        if (index < 0) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.viewportSize.height > 0 }.first { it }
        listState.centerItem(index)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surface)
            .nestedScroll(chromeScrollConnection)
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
        // Vibrant themed Catmull-Rom mesh behind the ayahs — the Flow player's backdrop. A warped 3×3
        // grid of accent colors, low-alpha so the centred ayah stays high-contrast; the normal reader
        // page stays flat (this is the player, a deliberate exception).
        SurauMeshGradient(
            gridWidth = 3,
            gridHeight = 3,
            points = FlowMeshPoints,
            colors = flowMesh,
            modifier = Modifier.fillMaxSize(),
            subdivisions = 14,
        )
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
                        listState = listState,
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

        AnimatedVisibility(
            visible = chromeVisible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = slideInVertically(chromeSlideSpec) { -it } + fadeIn(chromeFadeSpec),
            exit = slideOutVertically(chromeSlideSpec) { -it } + fadeOut(chromeFadeSpec),
        ) {
            FlowTopBar(
                surahName = success.surahName,
                sleepTimerRemainingMs = playerState.sleepTimerRemainingMs,
                stopAtSurahEnd = playerState.stopAtSurahEnd,
                embedded = embedded,
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
            enter = slideInVertically(chromeSlideSpec) { it } + fadeIn(chromeFadeSpec),
            exit = slideOutVertically(chromeSlideSpec) { it } + fadeOut(chromeFadeSpec),
        ) {
            FlowBottomBar(
                playerState = playerState,
                onPlayPause = onPlayPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onSetRepeat = onSetRepeat,
                onSetSleepTimer = onSetSleepTimer,
                onInteraction = { interactionTick++ },
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
            autoContinue = autoContinue,
            keepScreenOn = keepScreenOn,
            speed = playerState.speed,
            repeatScope = playerState.repeatScope,
            repeatCount = playerState.repeatCount,
            onFontScaleChange = onSetFontScale,
            onToggleTranslation = onToggleTranslation,
            onToggleAutoContinue = onToggleAutoContinue,
            onToggleKeepScreenOn = onToggleKeepScreenOn,
            onSetSpeed = onSetSpeed,
            onSetRepeat = onSetRepeat,
            onDismiss = { showSettings = false },
        )
    }
}

/**
 * The 9 colors of the Flow backdrop's 3×3 mesh, derived from the active (themed) scheme. Mixing
 * primary / secondary / tertiary across the grid gives the mesh real hue variety (so it reads as a
 * mesh, not one flat tint), while the low alpha keeps the recited ayah's onSurface ink legible.
 */
@Composable
private fun flowMeshColors(): List<Color> {
    val cs = MaterialTheme.colorScheme
    val dark = cs.surface.luminance() < 0.5f
    val a = if (dark) 0.24f else 0.32f
    return listOf(
        cs.primary.copy(alpha = a), cs.tertiary.copy(alpha = a), cs.secondary.copy(alpha = a),
        cs.secondary.copy(alpha = a), cs.primaryContainer.copy(alpha = a * 0.9f), cs.primary.copy(alpha = a),
        cs.tertiary.copy(alpha = a), cs.primary.copy(alpha = a), cs.secondary.copy(alpha = a),
    )
}

/** A 3×3 control grid with interior/edge points nudged off-axis so the mesh flows organically. */
private val FlowMeshPoints = listOf(
    Offset(0f, 0f), Offset(0.55f, 0f), Offset(1f, 0f),
    Offset(0f, 0.45f), Offset(0.62f, 0.5f), Offset(1f, 0.55f),
    Offset(0f, 1f), Offset(0.45f, 1f), Offset(1f, 1f),
)

@Composable
private fun FlowAyah(
    populated: PopulatedAyah,
    isActive: Boolean,
    fontScale: Float,
    showTranslation: Boolean,
    listState: LazyListState,
    onTap: () -> Unit,
) {
    val ayahNumber = populated.ayah.ayahNumber
    val color = if (isActive) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                // Picker-wheel effect: the ayah at the viewport centre is full size and opaque;
                // ayahs above/below shrink, fade, and tilt away like a rotating cylinder.
                val info = listState.layoutInfo
                val item = info.visibleItemsInfo.firstOrNull { it.key == ayahNumber }
                val viewportHeight = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
                if (item != null && viewportHeight > 0f) {
                    val viewportCenter = info.viewportStartOffset + viewportHeight / 2f
                    val itemCenter = item.offset + item.size / 2f
                    val fraction = (abs(itemCenter - viewportCenter) / (viewportHeight / 2f))
                        .coerceIn(0f, 1f)
                    val scale = lerp(1f, 0.5f, fraction)
                    scaleX = scale
                    scaleY = scale
                    alpha = lerp(1f, 0.15f, fraction)
                    cameraDistance = 16f * density
                    rotationX = lerp(0f, 52f, fraction) * if (itemCenter < viewportCenter) 1f else -1f
                }
            }
            .clickable(onClick = onTap)
            .padding(horizontal = 24.dp, vertical = 18.dp)
            .testTag("flow:ayah:$ayahNumber"),
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
    sleepTimerRemainingMs: Long?,
    stopAtSurahEnd: Boolean,
    embedded: Boolean,
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
                    imageVector = if (embedded) SurauIcons.ChevronDown else SurauIcons.ArrowBack,
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
            // Ambient sleep-timer readout while a stop is armed.
            if (sleepTimerRemainingMs != null || stopAtSurahEnd) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp).testTag("flow:sleepIndicator"),
                ) {
                    Icon(
                        imageVector = SurauIcons.Bedtime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = sleepTimerRemainingMs?.let(::formatTimer)
                            ?: stringResource(R.string.feature_quran_impl_sleep_end_of_surah),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
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
    onSetRepeat: (RepeatScope, Int) -> Unit,
    onSetSleepTimer: (SleepTimerOption) -> Unit,
    onInteraction: () -> Unit,
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
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RepeatButton(
                    scope = playerState.repeatScope,
                    onCycle = {
                        onSetRepeat(playerState.repeatScope.next(), playerState.repeatCount)
                        onInteraction()
                    },
                )
                IconButton(onClick = {
                    onPrevious()
                    onInteraction()
                }) {
                    Icon(
                        imageVector = SurauIcons.SkipPrevious,
                        contentDescription = stringResource(R.string.feature_quran_impl_previous_ayah),
                    )
                }
                FilledTonalIconButton(
                    onClick = {
                        onPlayPause()
                        onInteraction()
                    },
                    modifier = Modifier.testTag("flow:playPause"),
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
                IconButton(onClick = {
                    onNext()
                    onInteraction()
                }) {
                    Icon(
                        imageVector = SurauIcons.SkipNext,
                        contentDescription = stringResource(R.string.feature_quran_impl_next_ayah),
                    )
                }
                SleepTimerButton(
                    active = playerState.sleepTimerRemainingMs != null || playerState.stopAtSurahEnd,
                    onSelect = {
                        onSetSleepTimer(it)
                        onInteraction()
                    },
                )
            }
        }
    }
}

@Composable
private fun RepeatButton(scope: RepeatScope, onCycle: () -> Unit) {
    IconButton(onClick = onCycle, modifier = Modifier.testTag("flow:repeat")) {
        Icon(
            imageVector = if (scope == RepeatScope.AYAH) SurauIcons.RepeatOne else SurauIcons.Repeat,
            contentDescription = stringResource(R.string.feature_quran_impl_repeat_action),
            tint = if (scope == RepeatScope.OFF) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
    }
}

@Composable
private fun SleepTimerButton(active: Boolean, onSelect: (SleepTimerOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.testTag("flow:sleepTimer")) {
            Icon(
                imageVector = SurauIcons.Bedtime,
                contentDescription = stringResource(R.string.feature_quran_impl_sleep_timer),
                tint = if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (minutes in SLEEP_TIMER_MINUTES) {
                DropdownMenuItem(
                    text = {
                        Text(stringResource(R.string.feature_quran_impl_sleep_minutes, minutes))
                    },
                    onClick = {
                        expanded = false
                        onSelect(SleepTimerOption.After(minutes * 60_000L))
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.feature_quran_impl_sleep_end_of_surah)) },
                onClick = {
                    expanded = false
                    onSelect(SleepTimerOption.EndOfSurah)
                },
            )
            if (active) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.feature_quran_impl_sleep_off)) },
                    onClick = {
                        expanded = false
                        onSelect(SleepTimerOption.Off)
                    },
                )
            }
        }
    }
}

private fun RepeatScope.next(): RepeatScope = when (this) {
    RepeatScope.OFF -> RepeatScope.AYAH
    RepeatScope.AYAH -> RepeatScope.SURAH
    RepeatScope.SURAH -> RepeatScope.OFF
}

private fun formatTimer(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private fun formatSpeed(speed: Float): String {
    val text = if (speed == speed.toInt().toFloat()) speed.toInt().toString() else speed.toString()
    return "$text×"
}

@Composable
private fun FlowSettingsSheet(
    fontScale: Float,
    showTranslation: Boolean,
    autoContinue: Boolean,
    keepScreenOn: Boolean,
    speed: Float,
    repeatScope: RepeatScope,
    repeatCount: Int,
    onFontScaleChange: (Float) -> Unit,
    onToggleTranslation: () -> Unit,
    onToggleAutoContinue: () -> Unit,
    onToggleKeepScreenOn: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetRepeat: (RepeatScope, Int) -> Unit,
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
                SurauSwitch(checked = showTranslation, onCheckedChange = { onToggleTranslation() })
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.feature_quran_impl_flow_auto_continue),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                SurauSwitch(checked = autoContinue, onCheckedChange = { onToggleAutoContinue() })
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.feature_quran_impl_flow_keep_screen_on),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                SurauSwitch(checked = keepScreenOn, onCheckedChange = { onToggleKeepScreenOn() })
            }

            Text(
                text = stringResource(R.string.feature_quran_impl_flow_speed),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 20.dp),
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (option in SPEEDS) {
                    FilterChip(
                        selected = abs(speed - option) < 0.01f,
                        onClick = { onSetSpeed(option) },
                        label = { Text(formatSpeed(option)) },
                    )
                }
            }

            Text(
                text = stringResource(R.string.feature_quran_impl_repeat),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 20.dp),
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = repeatScope == RepeatScope.OFF,
                    onClick = { onSetRepeat(RepeatScope.OFF, repeatCount) },
                    label = { Text(stringResource(R.string.feature_quran_impl_repeat_off)) },
                )
                FilterChip(
                    selected = repeatScope == RepeatScope.AYAH,
                    onClick = { onSetRepeat(RepeatScope.AYAH, repeatCount) },
                    label = { Text(stringResource(R.string.feature_quran_impl_repeat_ayah)) },
                )
                FilterChip(
                    selected = repeatScope == RepeatScope.SURAH,
                    onClick = { onSetRepeat(RepeatScope.SURAH, repeatCount) },
                    label = { Text(stringResource(R.string.feature_quran_impl_repeat_surah)) },
                )
            }
            if (repeatScope != RepeatScope.OFF) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (count in REPEAT_COUNTS) {
                        FilterChip(
                            selected = repeatCount == count,
                            onClick = { onSetRepeat(repeatScope, count) },
                            label = {
                                Text(
                                    if (count == 0) {
                                        stringResource(R.string.feature_quran_impl_repeat_infinite)
                                    } else {
                                        stringResource(R.string.feature_quran_impl_repeat_times, count)
                                    },
                                )
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

/** Scrolls so the centre of item [index] aligns with the viewport centre. */
private suspend fun LazyListState.centerItem(index: Int) {
    if (layoutInfo.visibleItemsInfo.any { it.index == index }) {
        animateScrollBy(distanceToCenter(index), animationSpec = spring(stiffness = Spring.StiffnessLow))
    } else {
        scrollToItem(index)
        scrollBy(distanceToCenter(index))
    }
}

/**
 * Signed pixels from item [index]'s centre to the viewport centre, or `0` if it is not laid out.
 * Uses the viewport's offset frame (`viewportStartOffset`/`viewportEndOffset`), which accounts for
 * the large content padding — not the raw `viewportSize` — so a positive result scrolls the item up
 * to the centre.
 */
private fun LazyListState.distanceToCenter(index: Int): Float {
    val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return 0f
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
    return item.offset + item.size / 2f - viewportCenter
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private val FADE_HEIGHT = 120.dp
private const val CHROME_IDLE_MS = 3_000L

/** Per-frame user-drag delta (px) past which a scroll reveals/hides the immersive chrome. */
private const val CHROME_SCROLL_THRESHOLD_PX = 8f
private val SLEEP_TIMER_MINUTES = listOf(15, 30, 45, 60)
private val REPEAT_COUNTS = listOf(0, 3, 5, 7) // 0 = unlimited
private val SPEEDS = listOf(0.75f, 1f, 1.25f, 1.5f)
