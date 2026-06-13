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

package org.surau.app.feature.quran.impl

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.surau.app.core.media.PlayerUiState
import org.surau.app.core.media.SurauPlayerController
import org.surau.app.core.model.data.quran.SurahAudioManifest

/** Test double recording [playSurah] calls and letting tests drive [state]. */
internal class FakeSurauPlayerController : SurauPlayerController {

    val playerState = MutableStateFlow(PlayerUiState())
    override val state: StateFlow<PlayerUiState> = playerState

    data class PlayCall(val manifest: SurahAudioManifest, val surahName: String, val startAyah: Int)

    val playCalls = mutableListOf<PlayCall>()
    val playedManifests get() = playCalls.map { it.manifest }

    override fun playSurah(manifest: SurahAudioManifest, surahName: String, startAyah: Int) {
        playCalls += PlayCall(manifest, surahName, startAyah)
    }

    override fun playPause() = Unit
    override fun next() = Unit
    override fun previous() = Unit
    override fun seekToAyah(ayahNumber: Int) = Unit
    override fun stop() = Unit
}
