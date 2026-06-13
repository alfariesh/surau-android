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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.surau.app.core.common.result.Result
import org.surau.app.core.common.result.asResult
import org.surau.app.core.data.repository.QuranAudioRepository
import org.surau.app.core.data.repository.QuranProgressRepository
import org.surau.app.core.data.repository.QuranRepository
import org.surau.app.core.data.repository.UserDataRepository
import org.surau.app.core.domain.GetReaderContentUseCase
import org.surau.app.core.domain.ReaderContent
import org.surau.app.core.media.PlayerUiState
import org.surau.app.core.media.SurauPlayerController
import org.surau.app.core.model.data.quran.AyahKey
import org.surau.app.core.model.data.quran.ReaderMode
import org.surau.app.core.model.data.quran.Recitation
import org.surau.app.core.model.data.quran.TranslationSource
import org.surau.app.core.network.model.SurauApiException
import org.surau.app.feature.quran.api.navigation.SurahReaderNavKey
import java.io.IOException

@HiltViewModel(assistedFactory = SurahReaderViewModel.Factory::class)
class SurahReaderViewModel @AssistedInject constructor(
    getReaderContent: GetReaderContentUseCase,
    quranRepository: QuranRepository,
    private val quranProgressRepository: QuranProgressRepository,
    private val userDataRepository: UserDataRepository,
    private val quranAudioRepository: QuranAudioRepository,
    private val playerController: SurauPlayerController,
    @Assisted val navKey: SurahReaderNavKey,
) : ViewModel() {

    val translationSources: StateFlow<List<TranslationSource>> =
        quranRepository.observeTranslationSources()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    val recitations: StateFlow<List<Recitation>> =
        quranAudioRepository.observeRecitations()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    val selectedRecitationId: StateFlow<String?> =
        userDataRepository.userData
            .map { it.recitationId }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null,
            )

    val playerState: StateFlow<PlayerUiState> =
        playerController.state
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = PlayerUiState(),
            )

    /** The playing ayah when this surah's session is active, else `null` (gates the highlight). */
    val playingAyah: StateFlow<Int?> =
        playerController.state
            .map { if (it.surahId == navKey.surahId) it.currentAyahNumber else null }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null,
            )

    private val _audioError = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits when a manifest fetch fails (offline / backend error) — prompts a snackbar. */
    val audioError: SharedFlow<Unit> = _audioError

    private val _audioPartial = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits when the surah has missing ayah audio — prompts a one-time snackbar. */
    val audioPartial: SharedFlow<Unit> = _audioPartial

    @AssistedFactory
    interface Factory {
        fun create(navKey: SurahReaderNavKey): SurahReaderViewModel
    }

    private val visibleAyah = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<ReaderUiState> =
        getReaderContent(navKey.surahId)
            .asResult()
            .map { result ->
                when (result) {
                    is Result.Loading -> ReaderUiState.Loading
                    is Result.Error -> ReaderUiState.Error
                    is Result.Success ->
                        if (result.data.surah == null || result.data.ayahs.isEmpty()) {
                            ReaderUiState.Error
                        } else {
                            ReaderUiState.Success(
                                content = result.data,
                                initialAyahNumber = navKey.ayahNumber,
                            )
                        }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ReaderUiState.Loading,
            )

    init {
        // Persist the most-visible ayah as the reading position, debounced so casual scrolling
        // doesn't spam writes; push to the backend (no-op for guests).
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            visibleAyah
                .filterNotNull()
                .distinctUntilChanged()
                .debounce(POSITION_DEBOUNCE_MS)
                .collect { ayahNumber ->
                    quranProgressRepository.savePosition(
                        AyahKey.of(navKey.surahId, ayahNumber),
                    )
                    quranProgressRepository.pushPendingPosition()
                }
        }
    }

    /** Called by the reader list when the most-visible ayah changes. */
    fun onAyahVisible(ayahNumber: Int) {
        visibleAyah.value = ayahNumber
    }

    fun setReaderMode(mode: ReaderMode) {
        viewModelScope.launch { userDataRepository.setReaderMode(mode) }
    }

    fun setArabicFontScale(scale: Float) {
        viewModelScope.launch { userDataRepository.setArabicFontScale(scale) }
    }

    fun setTranslationSource(sourceId: String) {
        viewModelScope.launch { userDataRepository.setTranslationSourceId(sourceId) }
    }

    /** Plays from [ayahNumber]: seeks if this surah is already loaded, else fetches + plays. */
    fun playFromAyah(ayahNumber: Int) {
        viewModelScope.launch {
            val current = playerController.state.value
            if (current.surahId == navKey.surahId) {
                playerController.seekToAyah(ayahNumber)
                if (!current.isPlaying) playerController.playPause()
            } else {
                loadAndPlay(ayahNumber)
            }
        }
    }

    fun onPlayPause() = playerController.playPause()

    fun onNext() = playerController.next()

    fun onPrevious() = playerController.previous()

    /** Switches the reciter, restarting the current ayah with the new manifest if playing. */
    fun setRecitation(recitationId: String) {
        viewModelScope.launch {
            userDataRepository.setRecitationId(recitationId)
            val current = playerController.state.value
            if (current.surahId == navKey.surahId) {
                current.currentAyahNumber?.let { loadAndPlay(it) }
            }
        }
    }

    private suspend fun loadAndPlay(ayahNumber: Int) {
        val surahName =
            (uiState.value as? ReaderUiState.Success)?.content?.surah?.nameLatin.orEmpty()
        val recitationId = quranAudioRepository.resolveRecitationId(
            userDataRepository.userData.first().recitationId,
        )
        val manifest = try {
            quranAudioRepository.audioManifest(navKey.surahId, recitationId)
        } catch (error: IOException) {
            _audioError.tryEmit(Unit)
            return
        } catch (error: SurauApiException) {
            _audioError.tryEmit(Unit)
            return
        }
        if (manifest.missingAyahKeys.isNotEmpty()) _audioPartial.tryEmit(Unit)
        playerController.playSurah(manifest, surahName, ayahNumber)
    }

    companion object {
        private const val POSITION_DEBOUNCE_MS = 1_500L
    }
}

sealed interface ReaderUiState {
    data object Loading : ReaderUiState

    data object Error : ReaderUiState

    data class Success(
        val content: ReaderContent,
        val initialAyahNumber: Int?,
    ) : ReaderUiState
}
