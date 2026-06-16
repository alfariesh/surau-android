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

package org.surau.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.surau.app.core.data.repository.AuthRepository
import org.surau.app.core.data.repository.QuranAudioRepository
import org.surau.app.core.data.repository.QuranRepository
import org.surau.app.core.data.repository.UserDataRepository
import org.surau.app.core.data.repository.UserRepository
import org.surau.app.core.data.util.QuranDownloadManager
import org.surau.app.core.data.util.QuranDownloadState
import org.surau.app.core.model.data.DarkThemeConfig
import org.surau.app.core.model.data.ThemeContrast
import org.surau.app.core.model.data.ThemeStyle
import org.surau.app.core.model.data.auth.AuthState
import org.surau.app.core.model.data.quran.ReaderMode
import org.surau.app.core.model.data.quran.Recitation
import org.surau.app.core.model.data.quran.TranslationSource
import org.surau.app.ui.profile.ProfileUiState.Loading
import org.surau.app.ui.profile.ProfileUiState.Success
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Backs the Profile hub, which is now the single home for every user-editable setting (theme,
 * language, reader, recitation/translation, offline download) plus the account actions. It absorbs
 * what used to live in the standalone Settings screen.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val quranDownloadManager: QuranDownloadManager,
    quranRepository: QuranRepository,
    quranAudioRepository: QuranAudioRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> =
        combine(
            userDataRepository.userData,
            authRepository.authState,
            quranRepository.observeTranslationSources(),
            quranAudioRepository.observeRecitations(),
            quranDownloadManager.downloadState,
        ) { userData, authState, translationSources, recitations, downloadState ->
            Success(
                settings = UserEditableSettings(
                    useDynamicColor = userData.useDynamicColor,
                    darkThemeConfig = userData.darkThemeConfig,
                    seedColorArgb = userData.seedColorArgb,
                    themeStyle = userData.themeStyle,
                    themeContrast = userData.themeContrast,
                    useMeshGradient = userData.useMeshGradient,
                    readerMode = userData.readerMode,
                    translationSourceId = userData.translationSourceId,
                    recitationId = userData.recitationId,
                    arabicFontScale = userData.arabicFontScale,
                    showTransliteration = userData.readerShowTransliteration,
                    showTranslation = userData.readerShowTranslation,
                    arabicLineSpacing = userData.readerArabicLineSpacing,
                    translationScale = userData.readerTranslationScale,
                    keepScreenOn = userData.readerKeepScreenOn,
                ),
                authState = authState,
                translationSources = translationSources,
                recitations = recitations,
                quranDownloadState = downloadState,
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = WhileSubscribed(5.seconds.inWholeMilliseconds),
                initialValue = Loading,
            )

    fun updateDarkThemeConfig(darkThemeConfig: DarkThemeConfig) {
        viewModelScope.launch {
            userDataRepository.setDarkThemeConfig(darkThemeConfig)
        }
    }

    fun updateDynamicColorPreference(useDynamicColor: Boolean) {
        viewModelScope.launch {
            userDataRepository.setDynamicColorPreference(useDynamicColor)
        }
    }

    /**
     * Applies a preset/custom theme seed (0 resets to the default scheme). Choosing a seed is
     * mutually exclusive with wallpaper-based dynamic color, so it is turned off here.
     */
    fun updateThemeSeed(argb: Long) {
        viewModelScope.launch {
            userDataRepository.setSeedColor(argb)
            if (argb != 0L) {
                userDataRepository.setDynamicColorPreference(false)
            }
        }
    }

    fun updateThemeStyle(themeStyle: ThemeStyle) {
        viewModelScope.launch {
            userDataRepository.setThemeStyle(themeStyle)
        }
    }

    fun updateThemeContrast(themeContrast: ThemeContrast) {
        viewModelScope.launch {
            userDataRepository.setThemeContrast(themeContrast)
        }
    }

    fun updateMeshGradient(useMeshGradient: Boolean) {
        viewModelScope.launch {
            userDataRepository.setMeshGradientPreference(useMeshGradient)
        }
    }

    fun updateReaderMode(readerMode: ReaderMode) {
        viewModelScope.launch {
            userDataRepository.setReaderMode(readerMode)
            userRepository.pushReaderPreferences()
        }
    }

    fun updateTranslationSource(sourceId: String) {
        viewModelScope.launch {
            userDataRepository.setTranslationSourceId(sourceId)
            userRepository.pushReaderPreferences()
        }
    }

    fun updateRecitation(recitationId: String) {
        viewModelScope.launch {
            userDataRepository.setRecitationId(recitationId)
            userRepository.pushReaderPreferences()
        }
    }

    fun updateArabicFontScale(scale: Float) {
        viewModelScope.launch {
            userDataRepository.setArabicFontScale(scale)
        }
    }

    fun updateShowTransliteration(show: Boolean) {
        viewModelScope.launch { userDataRepository.setReaderShowTransliteration(show) }
    }

    fun updateShowTranslation(show: Boolean) {
        viewModelScope.launch { userDataRepository.setReaderShowTranslation(show) }
    }

    fun updateArabicLineSpacing(spacing: Float) {
        viewModelScope.launch { userDataRepository.setReaderArabicLineSpacing(spacing) }
    }

    fun updateTranslationScale(scale: Float) {
        viewModelScope.launch { userDataRepository.setReaderTranslationScale(scale) }
    }

    fun updateKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { userDataRepository.setReaderKeepScreenOn(enabled) }
    }

    fun startQuranDownload() = quranDownloadManager.startDownload()

    fun cancelQuranDownload() = quranDownloadManager.cancelDownload()

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}

/**
 * Represents the settings which the user can edit within the Profile hub.
 */
data class UserEditableSettings(
    val useDynamicColor: Boolean,
    val darkThemeConfig: DarkThemeConfig,
    val readerMode: ReaderMode,
    val translationSourceId: String?,
    val arabicFontScale: Float,
    val seedColorArgb: Long = 0L,
    val themeStyle: ThemeStyle = ThemeStyle.TONAL_SPOT,
    val themeContrast: ThemeContrast = ThemeContrast.STANDARD,
    val useMeshGradient: Boolean = false,
    val recitationId: String? = null,
    val showTransliteration: Boolean = false,
    val showTranslation: Boolean = true,
    val arabicLineSpacing: Float = 1f,
    val translationScale: Float = 1f,
    val keepScreenOn: Boolean = true,
)

sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    data class Success(
        val settings: UserEditableSettings,
        val authState: AuthState,
        val translationSources: List<TranslationSource>,
        val recitations: List<Recitation> = emptyList(),
        val quranDownloadState: QuranDownloadState = QuranDownloadState.Idle,
    ) : ProfileUiState
}
