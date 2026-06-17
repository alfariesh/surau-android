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

package org.surau.app.core.media

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.surau.app.core.model.data.quran.SurahAudioManifest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps an async [MediaController] connected to [PlaybackService] and exposes a [StateFlow] of
 * [PlayerUiState]. The controller is connected lazily on the first command and never proactively
 * released; the OS reclaims the bound service on process death and [PlaybackService.onTaskRemoved]
 * handles swipe-away.
 *
 * All controller access runs on the main thread (Media3 requirement); commands issued before the
 * connection completes simply suspend in [awaitController] until it is ready.
 */
@Singleton
class DefaultSurauPlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
) : SurauPlayerController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(PlayerUiState())
    override val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val _surahCompletions = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    override val surahCompletions: SharedFlow<Int> = _surahCompletions.asSharedFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var positionJob: Job? = null

    /** Non-null while a surah-mode session is loaded; maps playback position to the active ayah. */
    private var ayahTimeline: AyahTimeline? = null

    // Surah-mode repeat (the latch/count/boundary logic lives in the testable [RepeatLoop]);
    // loopScope is kept here only for the UI display and to gate single-ayah follow.
    private val repeat = RepeatLoop()
    private var loopScope = RepeatScope.OFF

    private var sleepTimerJob: Job? = null
    private var stopAtSurahEnd = false

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            syncState()
            pollPosition(isPlaying)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = syncState()

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) = syncState()

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                when {
                    stopAtSurahEnd -> {
                        stopAtSurahEnd = false
                        _state.update { it.copy(stopAtSurahEnd = false) }
                        controller?.pause()
                    }

                    repeat.isActive -> onLoopReachedEnd()

                    else -> {
                        // Natural end (not looping, not a sleep stop): signal for auto-advance.
                        _state.value.surahId?.let { _surahCompletions.tryEmit(it) }
                    }
                }
            }
            syncState()
        }

        override fun onPlayerError(error: PlaybackException) = syncState()
    }

    override fun playSurah(manifest: SurahAudioManifest, surahName: String, startAyah: Int) =
        controllerAction { controller ->
            // A new surah invalidates any ayah-range loop and any in-progress fade.
            clearRepeat()
            controller.volume = 1f
            if (manifest.mode == MODE_SURAH) {
                // One audio file for the whole surah; the active ayah comes from the timeline.
                val timeline = AyahTimeline.from(manifest)
                val item = manifest.toSurahModeMediaItem(surahName, timeline.startTimes())
                    ?: return@controllerAction
                ayahTimeline = timeline
                controller.setMediaItems(listOf(item), 0, timeline.startMsOf(startAyah) ?: 0L)
            } else {
                ayahTimeline = null
                val items = manifest.toMediaItems(surahName)
                if (items.isEmpty()) return@controllerAction
                val startIndex = items
                    .indexOfFirst { it.mediaId == "${manifest.surahId}:$startAyah" }
                    .coerceAtLeast(0)
                controller.setMediaItems(items, startIndex, 0L)
            }
            controller.prepare()
            controller.playWhenReady = true
        }

    override fun playPause() = controllerAction { if (it.isPlaying) it.pause() else it.play() }

    override fun next() = controllerAction { controller ->
        val timeline = ayahTimeline
        if (timeline != null) {
            val current = timeline.ayahAt(controller.currentPosition.coerceAtLeast(0L))
                ?: return@controllerAction
            val target = timeline.nextAyah(current) ?: return@controllerAction // last ayah: no wrap
            controller.seekTo(timeline.startMsOf(target) ?: 0L)
            followAyahLoop(target)
            syncState()
        } else if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        }
    }

    override fun previous() = controllerAction { controller ->
        val timeline = ayahTimeline
        if (timeline != null) {
            val position = controller.currentPosition.coerceAtLeast(0L)
            val current = timeline.ayahAt(position) ?: return@controllerAction
            val currentStart = timeline.startMsOf(current) ?: 0L
            // Restart the current ayah if past its start, otherwise step to the previous ayah.
            val target = if (position - currentStart > RESTART_THRESHOLD_MS) {
                current
            } else {
                timeline.prevAyah(current) ?: current
            }
            controller.seekTo(timeline.startMsOf(target) ?: 0L)
            followAyahLoop(target)
            syncState()
        } else {
            controller.seekToPrevious()
        }
    }

    override fun seekToAyah(ayahNumber: Int) = controllerAction { controller ->
        val timeline = ayahTimeline
        if (timeline != null) {
            val ms = timeline.startMsOf(ayahNumber) ?: return@controllerAction
            controller.seekTo(ms)
            followAyahLoop(ayahNumber)
            syncState()
        } else {
            val index = (0 until controller.mediaItemCount).firstOrNull { i ->
                controller.getMediaItemAt(i).mediaId.substringAfter(':', "").toIntOrNull() == ayahNumber
            } ?: return@controllerAction
            controller.seekToDefaultPosition(index)
        }
    }

    override fun setRepeat(scope: RepeatScope, count: Int) = controllerAction { controller ->
        val timeline = ayahTimeline
        val range = when {
            scope == RepeatScope.OFF || timeline == null -> null
            scope == RepeatScope.AYAH ->
                currentAyah(controller.currentPosition.coerceAtLeast(0L))?.let { it..it }
            else -> { // SURAH
                val first = timeline.firstAyah()
                val last = timeline.lastAyah()
                if (first != null && last != null) first..last else null
            }
        }
        val target = count.coerceAtLeast(0)
        repeat.start(range, target)
        loopScope = if (range != null) scope else RepeatScope.OFF
        // A loop and an "end of surah" stop are mutually exclusive.
        if (range != null) stopAtSurahEnd = false
        _state.update {
            it.copy(repeatScope = loopScope, repeatCount = target, stopAtSurahEnd = stopAtSurahEnd)
        }
    }

    override fun setSleepTimer(option: SleepTimerOption) {
        sleepTimerJob?.cancel()
        // Undo any in-progress fade on the player thread (only if already connected — arming a timer
        // before playback shouldn't force a service bind just to reset the volume).
        if (controller != null) controllerAction { it.volume = 1f }
        when (option) {
            SleepTimerOption.Off -> {
                stopAtSurahEnd = false
                _state.update { it.copy(sleepTimerRemainingMs = null, stopAtSurahEnd = false) }
            }

            is SleepTimerOption.After -> {
                stopAtSurahEnd = false
                sleepTimerJob = scope.launch {
                    var remaining = option.durationMs
                    while (remaining > 0) {
                        _state.update { it.copy(sleepTimerRemainingMs = remaining, stopAtSurahEnd = false) }
                        delay(SLEEP_TICK_MS)
                        remaining -= SLEEP_TICK_MS
                    }
                    _state.update { it.copy(sleepTimerRemainingMs = null) }
                    fadeOutAndPause()
                }
            }

            SleepTimerOption.EndOfSurah -> {
                clearRepeat() // a loop would never let the surah end
                stopAtSurahEnd = true
                _state.update { it.copy(sleepTimerRemainingMs = null, stopAtSurahEnd = true) }
            }
        }
    }

    override fun setSpeed(speed: Float) = controllerAction { it.setPlaybackSpeed(speed) }

    override fun stop() = controllerAction {
        it.stop()
        it.clearMediaItems()
        it.volume = 1f
        ayahTimeline = null
        clearRepeat()
        sleepTimerJob?.cancel()
        stopAtSurahEnd = false
        _state.update { state -> state.copy(sleepTimerRemainingMs = null, stopAtSurahEnd = false) }
    }

    /**
     * Releases the controller and unbinds the service. Intentionally NOT called during normal
     * operation: this @Singleton keeps the [MediaController] for the whole process so background
     * playback survives leaving any screen, and the OS reclaims the bound service on process death /
     * swipe-away ([PlaybackService.onTaskRemoved]). The retained binding is a deliberate trade-off.
     */
    fun release() {
        positionJob?.cancel()
        sleepTimerJob?.cancel()
        controller?.removeListener(listener)
        controller?.release()
        controllerFuture?.let(MediaController::releaseFuture)
        controller = null
        controllerFuture = null
    }

    private fun controllerAction(block: (MediaController) -> Unit) {
        scope.launch { block(awaitController()) }
    }

    private suspend fun awaitController(): MediaController {
        controller?.let { return it }
        val future = controllerFuture ?: run {
            val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            MediaController.Builder(context, token).buildAsync().also { controllerFuture = it }
        }
        val connected = future.await()
        if (controller == null) {
            connected.addListener(listener)
            controller = connected
            syncState()
        }
        return controller ?: connected
    }

    /** The active ayah: from the surah-mode [ayahTimeline] if loaded, else the MediaItem's id. */
    private fun currentAyah(positionMs: Long): Int? {
        ayahTimeline?.let { return it.ayahAt(positionMs) }
        return controller?.currentMediaItem?.mediaId?.substringAfter(':', "")?.toIntOrNull()
    }

    /** When looping a single ayah, moves the loop to follow explicit ayah navigation. */
    private fun followAyahLoop(ayah: Int) {
        if (loopScope != RepeatScope.AYAH) return
        repeat.retargetSingle(ayah)
    }

    private fun clearRepeat() {
        repeat.stop()
        markRepeatStateOff()
    }

    /** Syncs the UI-facing repeat state to OFF (the [RepeatLoop] is reset separately). */
    private fun markRepeatStateOff() {
        loopScope = RepeatScope.OFF
        _state.update { it.copy(repeatScope = RepeatScope.OFF, repeatCount = 0) }
    }

    /** Poll-driven repeat enforcement; see [RepeatLoop] for the latch/count semantics. */
    private fun enforceRepeat(player: MediaController, position: Long) {
        val range = repeat.range ?: return
        val timeline = ayahTimeline ?: return
        val endMs = timeline.endMsOf(range.last) ?: player.duration.coerceAtLeast(0L)
        when (repeat.onPosition(position, endMs)) {
            RepeatAction.SeekToStart -> player.seekTo(timeline.startMsOf(range.first) ?: 0L)
            RepeatAction.Finished -> markRepeatStateOff()
            RepeatAction.None -> Unit
        }
    }

    /**
     * Backstop for an end-of-file loop boundary the 250ms poll missed: when the player reports
     * STATE_ENDED while a loop is active, restart the range here (or finish if the count is reached).
     */
    private fun onLoopReachedEnd() {
        val range = repeat.range ?: return
        when (repeat.onEnded()) {
            RepeatAction.SeekToStart -> controller?.run {
                seekTo(ayahTimeline?.startMsOf(range.first) ?: 0L)
                play()
            }
            RepeatAction.Finished -> {
                markRepeatStateOff()
                // Requested repeats done; the surah ended naturally, so allow auto-advance.
                _state.value.surahId?.let { _surahCompletions.tryEmit(it) }
            }
            RepeatAction.None -> Unit
        }
    }

    private suspend fun fadeOutAndPause() {
        val player = controller ?: return
        val startVolume = player.volume
        for (step in 1..SLEEP_FADE_STEPS) {
            player.volume = startVolume * (1f - step.toFloat() / SLEEP_FADE_STEPS)
            delay(SLEEP_FADE_MS / SLEEP_FADE_STEPS)
        }
        player.pause()
        player.volume = startVolume // restore for the next play
    }

    private fun syncState() {
        val controller = controller ?: return
        val position = controller.currentPosition.coerceAtLeast(0L)
        _state.update { previous ->
            reducePlayerState(
                base = previous,
                isPlaying = controller.isPlaying,
                currentItem = controller.currentMediaItem,
                durationMs = controller.duration,
            ).copy(
                positionMs = position,
                currentAyahNumber = currentAyah(position),
                speed = controller.playbackParameters.speed,
            )
        }
    }

    private fun pollPosition(isPlaying: Boolean) {
        positionJob?.cancel()
        if (!isPlaying) return
        positionJob = scope.launch {
            while (isActive) {
                controller?.let { player ->
                    val position = player.currentPosition.coerceAtLeast(0L)
                    _state.update {
                        it.copy(positionMs = position, currentAyahNumber = currentAyah(position))
                    }
                    enforceRepeat(player, position)
                }
                delay(POSITION_POLL_INTERVAL_MS)
            }
        }
    }

    private companion object {
        const val POSITION_POLL_INTERVAL_MS = 250L
        const val RESTART_THRESHOLD_MS = 1_000L
        const val MODE_SURAH = "surah"
        const val SLEEP_TICK_MS = 1_000L
        const val SLEEP_FADE_MS = 4_000L
        const val SLEEP_FADE_STEPS = 20
    }
}
