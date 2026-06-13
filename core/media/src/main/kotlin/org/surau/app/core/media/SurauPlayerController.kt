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

import kotlinx.coroutines.flow.StateFlow
import org.surau.app.core.model.data.quran.SurahAudioManifest

/**
 * App-wide handle on murottal playback, backed by a [PlaybackService]-owned player. Implementations
 * are process singletons so playback (and its notification) survives navigation and configuration
 * changes.
 */
interface SurauPlayerController {

    /** The current playback snapshot; `surahId == null` means no active session. */
    val state: StateFlow<PlayerUiState>

    /** Loads [manifest] and starts playback at [startAyah], replacing any current session. */
    fun playSurah(manifest: SurahAudioManifest, surahName: String, startAyah: Int)

    fun playPause()

    /** Advances to the next ayah; a no-op at the last ayah. */
    fun next()

    /** Restarts the current ayah if past its start, otherwise moves to the previous ayah. */
    fun previous()

    /** Seeks to [ayahNumber] within the loaded session, if present. */
    fun seekToAyah(ayahNumber: Int)

    /**
     * Loops the active surah-mode session per [scope], stopping after [count] plays (or repeating
     * forever when [count] is `0`). A no-op without a loaded surah-mode timeline.
     */
    fun setRepeat(scope: RepeatScope, count: Int)

    /** Arms (or with [SleepTimerOption.Off] clears) the sleep timer. */
    fun setSleepTimer(option: SleepTimerOption)

    fun stop()
}
