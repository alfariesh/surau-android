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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var positionJob: Job? = null

    /** Non-null while a surah-mode session is loaded; maps playback position to the active ayah. */
    private var ayahTimeline: AyahTimeline? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            syncState()
            pollPosition(isPlaying)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = syncState()

        override fun onPlaybackStateChanged(playbackState: Int) = syncState()

        override fun onPlayerError(error: PlaybackException) = syncState()
    }

    override fun playSurah(manifest: SurahAudioManifest, surahName: String, startAyah: Int) =
        controllerAction { controller ->
            if (manifest.mode == MODE_SURAH) {
                // One audio file for the whole surah; the active ayah comes from the timeline.
                val item = manifest.toSurahModeMediaItem(surahName) ?: return@controllerAction
                val timeline = AyahTimeline.from(manifest)
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
            val nextMs = timeline.nextAyah(current)?.let(timeline::startMsOf)
                ?: return@controllerAction // last ayah: no wrap
            controller.seekTo(nextMs)
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
            syncState()
        } else {
            val index = (0 until controller.mediaItemCount).firstOrNull { i ->
                controller.getMediaItemAt(i).mediaId.substringAfter(':', "").toIntOrNull() == ayahNumber
            } ?: return@controllerAction
            controller.seekToDefaultPosition(index)
        }
    }

    override fun stop() = controllerAction {
        it.stop()
        it.clearMediaItems()
        ayahTimeline = null
    }

    /** Releases the controller. Only needed on process teardown; unused during normal operation. */
    fun release() {
        positionJob?.cancel()
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

    private fun syncState() {
        val controller = controller ?: return
        val position = controller.currentPosition.coerceAtLeast(0L)
        _state.update { previous ->
            reducePlayerState(
                base = previous,
                isPlaying = controller.isPlaying,
                currentItem = controller.currentMediaItem,
                durationMs = controller.duration,
            ).copy(positionMs = position, currentAyahNumber = currentAyah(position))
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
                }
                delay(POSITION_POLL_INTERVAL_MS)
            }
        }
    }

    private companion object {
        const val POSITION_POLL_INTERVAL_MS = 250L
        const val RESTART_THRESHOLD_MS = 1_000L
        const val MODE_SURAH = "surah"
    }
}
