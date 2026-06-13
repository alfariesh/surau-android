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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
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
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = SurahFlowViewModel.Factory::class)
class SurahFlowViewModel @AssistedInject constructor(
    private val getReaderContent: GetReaderContentUseCase,
    private val quranAudioRepository: QuranAudioRepository,
    private val userDataRepository: UserDataRepository,
    private val playerController: SurauPlayerController,
    @Assisted val navKey: SurahFlowNavKey,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(navKey: SurahFlowNavKey): SurahFlowViewModel
    }

    /** The surah currently shown/playing; advances past [navKey] when auto-continue kicks in. */
    private val currentSurahId = MutableStateFlow(navKey.surahId)

    val uiState: StateFlow<FlowUiState> =
        currentSurahId
            .flatMapLatest { surahId ->
                getReaderContent(surahId)
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
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FlowUiState.Loading)

    val playerState: StateFlow<PlayerUiState> =
        playerController.state
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerUiState())

    /** The active ayah while the shown surah's session is playing, for centering. */
    val playingAyah: StateFlow<Int?> =
        combine(playerController.state, currentSurahId) { state, surahId ->
            if (state.surahId == surahId) state.currentAyahNumber else null
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val fontScale: StateFlow<Float> =
        userDataRepository.userData
            .map { it.flowArabicFontScale }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserData.DEFAULT_ARABIC_FONT_SCALE)

    val showTranslation: StateFlow<Boolean> =
        userDataRepository.userData
            .map { it.flowShowTranslation }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val autoContinue: StateFlow<Boolean> =
        userDataRepository.userData
            .map { it.flowAutoContinue }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _audioError = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val audioError: SharedFlow<Unit> = _audioError

    init {
        // Auto-advance to the next surah when the current one finishes (while Flow is foreground).
        viewModelScope.launch {
            playerController.surahCompletions.collect { endedSurahId ->
                // Read the pref freshly — the shared StateFlow can be unsubscribed (stale) here.
                val enabled = userDataRepository.userData.first().flowAutoContinue
                if (endedSurahId == currentSurahId.value && enabled && endedSurahId < LAST_SURAH) {
                    currentSurahId.value = endedSurahId + 1
                    loadAndPlay(endedSurahId + 1, startAyah = 1)
                }
            }
        }
        // Start the requested surah unless it is already playing (e.g. returning to Flow).
        viewModelScope.launch {
            if (playerController.state.value.surahId != currentSurahId.value) {
                loadAndPlay(currentSurahId.value, navKey.ayahNumber ?: 1)
            }
        }
    }

    /** Resolves the surah-mode reciter, fetches the manifest, and starts playback. */
    private suspend fun loadAndPlay(surahId: Int, startAyah: Int) {
        val surahName = getReaderContent(surahId).first().surah?.nameLatin.orEmpty()
        val recitationId = quranAudioRepository.resolveRecitationId(
            preferredId = null,
            requiredMode = MODE_SURAH,
        )
        val manifest = try {
            quranAudioRepository.audioManifest(surahId, recitationId)
        } catch (error: IOException) {
            _audioError.tryEmit(Unit)
            return
        } catch (error: SurauApiException) {
            _audioError.tryEmit(Unit)
            return
        }
        playerController.playSurah(manifest, surahName, startAyah)
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

    fun toggleAutoContinue() {
        viewModelScope.launch {
            userDataRepository.setFlowAutoContinue(!autoContinue.value)
        }
    }

    private companion object {
        const val MODE_SURAH = "surah"
        const val LAST_SURAH = 114
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
