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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.surau.app.core.common.result.Result
import org.surau.app.core.common.result.asResult
import org.surau.app.core.data.repository.QuranAudioRepository
import org.surau.app.core.data.repository.UserDataRepository
import org.surau.app.core.domain.GetReaderContentUseCase
import org.surau.app.core.media.PlayerUiState
import org.surau.app.core.media.RepeatScope
import org.surau.app.core.media.SleepTimerOption
import org.surau.app.core.media.SurauPlayerController
import org.surau.app.core.model.data.UserData
import org.surau.app.core.model.data.quran.PopulatedAyah
import org.surau.app.core.network.model.SurauApiException
import org.surau.app.feature.quran.api.navigation.SurahFlowNavKey
import java.io.IOException

/**
 * Drives the immersive Flow reader: loads the surah's ayahs, auto-plays the surah-mode recitation,
 * and exposes the active ayah (for centering) plus Flow-specific font/translation preferences.
 */
@HiltViewModel(assistedFactory = SurahFlowViewModel.Factory::class)
class SurahFlowViewModel @AssistedInject constructor(
    getReaderContent: GetReaderContentUseCase,
    private val quranAudioRepository: QuranAudioRepository,
    private val userDataRepository: UserDataRepository,
    private val playerController: SurauPlayerController,
    @Assisted val navKey: SurahFlowNavKey,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(navKey: SurahFlowNavKey): SurahFlowViewModel
    }

    val uiState: StateFlow<FlowUiState> =
        getReaderContent(navKey.surahId)
            .asResult()
            .map { result ->
                when (result) {
                    is Result.Loading -> FlowUiState.Loading
                    is Result.Error -> FlowUiState.Error
                    is Result.Success ->
                        if (result.data.surah == null || result.data.ayahs.isEmpty()) {
                            FlowUiState.Error
                        } else {
                            FlowUiState.Success(
                                ayahs = result.data.ayahs,
                                surahName = result.data.surah?.nameLatin.orEmpty(),
                                translationSourceId = result.data.translationSourceId,
                            )
                        }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FlowUiState.Loading)

    val playerState: StateFlow<PlayerUiState> =
        playerController.state
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerUiState())

    /** The active ayah while this surah's session is playing, for centering. */
    val playingAyah: StateFlow<Int?> =
        playerController.state
            .map { if (it.surahId == navKey.surahId) it.currentAyahNumber else null }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val fontScale: StateFlow<Float> =
        userDataRepository.userData
            .map { it.flowArabicFontScale }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserData.DEFAULT_ARABIC_FONT_SCALE)

    val showTranslation: StateFlow<Boolean> =
        userDataRepository.userData
            .map { it.flowShowTranslation }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _audioError = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val audioError: SharedFlow<Unit> = _audioError

    init {
        viewModelScope.launch {
            // Wait for the surah name before starting playback.
            val content = uiState.first { it !is FlowUiState.Loading }
            val surahName = (content as? FlowUiState.Success)?.surahName.orEmpty()
            // Already playing this surah (e.g. returning to Flow) → don't restart.
            if (playerController.state.value.surahId == navKey.surahId) return@launch
            val recitationId = quranAudioRepository.resolveRecitationId(
                preferredId = null,
                requiredMode = MODE_SURAH,
            )
            val manifest = try {
                quranAudioRepository.audioManifest(navKey.surahId, recitationId)
            } catch (error: IOException) {
                _audioError.tryEmit(Unit)
                return@launch
            } catch (error: SurauApiException) {
                _audioError.tryEmit(Unit)
                return@launch
            }
            playerController.playSurah(manifest, surahName, navKey.ayahNumber ?: 1)
        }
    }

    fun onPlayPause() = playerController.playPause()

    fun onNext() = playerController.next()

    fun onPrevious() = playerController.previous()

    fun onSeekToAyah(ayahNumber: Int) = playerController.seekToAyah(ayahNumber)

    fun onSetRepeat(scope: RepeatScope, count: Int) = playerController.setRepeat(scope, count)

    fun onSetSleepTimer(option: SleepTimerOption) = playerController.setSleepTimer(option)

    fun setFontScale(scale: Float) {
        viewModelScope.launch { userDataRepository.setFlowArabicFontScale(scale) }
    }

    fun toggleTranslation() {
        viewModelScope.launch {
            userDataRepository.setFlowShowTranslation(!showTranslation.value)
        }
    }

    private companion object {
        const val MODE_SURAH = "surah"
    }
}

sealed interface FlowUiState {
    data object Loading : FlowUiState

    data object Error : FlowUiState

    data class Success(
        val ayahs: List<PopulatedAyah>,
        val surahName: String,
        val translationSourceId: String,
    ) : FlowUiState
}
