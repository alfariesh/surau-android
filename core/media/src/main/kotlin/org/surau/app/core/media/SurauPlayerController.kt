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
 * App-wide handle on murottal playback. Wraps an async [MediaController] connected to
 * [PlaybackService] and exposes a [StateFlow] of [PlayerUiState]. The controller is a process
 * singleton so playback (and its notification) survives navigation and configuration changes — it
 * is connected lazily on the first command and never proactively released; the OS reclaims the
 * bound service on process death and [PlaybackService.onTaskRemoved] handles swipe-away.
 *
 * All controller access runs on the main thread (Media3 requirement); commands issued before the
 * connection completes simply suspend in [awaitController] until it is ready.
 */
@Singleton
class SurauPlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var positionJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            syncState()
            pollPosition(isPlaying)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = syncState()

        override fun onPlaybackStateChanged(playbackState: Int) = syncState()

        override fun onPlayerError(error: PlaybackException) = syncState()
    }

    /** Loads [manifest] and starts playback at [startAyah] (replacing any current session). */
    fun playSurah(manifest: SurahAudioManifest, surahName: String, startAyah: Int) =
        controllerAction { controller ->
            val items = manifest.toMediaItems(surahName)
            if (items.isEmpty()) return@controllerAction
            val startIndex = items
                .indexOfFirst { it.mediaId == "${manifest.surahId}:$startAyah" }
                .coerceAtLeast(0)
            // Start the selected ayah from position 0.
            controller.setMediaItems(items, startIndex, 0L)
            controller.prepare()
            controller.playWhenReady = true
        }

    fun playPause() = controllerAction { if (it.isPlaying) it.pause() else it.play() }

    /** Advances to the next ayah; does nothing at the last ayah (no wrap-around). */
    fun next() = controllerAction { if (it.hasNextMediaItem()) it.seekToNextMediaItem() }

    /** Restarts the current ayah if past the start, otherwise moves to the previous ayah. */
    fun previous() = controllerAction { it.seekToPrevious() }

    fun seekToAyah(ayahNumber: Int) = controllerAction { controller ->
        val index = (0 until controller.mediaItemCount).firstOrNull { i ->
            controller.getMediaItemAt(i).mediaId.substringAfter(':', "").toIntOrNull() == ayahNumber
        } ?: return@controllerAction
        controller.seekToDefaultPosition(index)
    }

    fun stop() = controllerAction {
        it.stop()
        it.clearMediaItems()
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

    private fun syncState() {
        val controller = controller ?: return
        _state.update { previous ->
            reducePlayerState(
                base = previous,
                isPlaying = controller.isPlaying,
                currentItem = controller.currentMediaItem,
                durationMs = controller.duration,
            ).copy(positionMs = controller.currentPosition.coerceAtLeast(0L))
        }
    }

    private fun pollPosition(isPlaying: Boolean) {
        positionJob?.cancel()
        if (!isPlaying) return
        positionJob = scope.launch {
            while (isActive) {
                controller?.let { player ->
                    _state.update { it.copy(positionMs = player.currentPosition.coerceAtLeast(0L)) }
                }
                delay(POSITION_POLL_INTERVAL_MS)
            }
        }
    }

    private companion object {
        const val POSITION_POLL_INTERVAL_MS = 500L
    }
}
