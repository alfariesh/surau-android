/*
 * Copyright 2022 The Android Open Source Project
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

package org.surau.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.surau.app.MainActivityUiState.Loading
import org.surau.app.MainActivityUiState.Success
import org.surau.app.core.data.repository.UserDataRepository
import org.surau.app.core.model.data.DarkThemeConfig
import org.surau.app.core.model.data.ThemeContrast
import org.surau.app.core.model.data.ThemePalette
import org.surau.app.core.model.data.ThemeStyle
import org.surau.app.core.model.data.UserData
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    userDataRepository: UserDataRepository,
) : ViewModel() {
    val uiState: StateFlow<MainActivityUiState> = userDataRepository.userData.map {
        Success(it)
    }.stateIn(
        scope = viewModelScope,
        initialValue = Loading,
        started = SharingStarted.WhileSubscribed(5_000),
    )
}

sealed interface MainActivityUiState {
    data object Loading : MainActivityUiState

    data class Success(val userData: UserData) : MainActivityUiState {
        override val shouldShowWelcome: Boolean = !userData.welcomeShown

        override val shouldDisableDynamicTheming = !userData.useDynamicColor

        override val seedColorArgb = userData.seedColorArgb

        override val themeStyle = userData.themeStyle

        override val themeContrast = userData.themeContrast

        override val themePalette = userData.themePalette

        override val useMeshGradient = userData.useMeshGradient

        override fun shouldUseDarkTheme(isSystemDarkTheme: Boolean) =
            when (userData.darkThemeConfig) {
                DarkThemeConfig.FOLLOW_SYSTEM -> isSystemDarkTheme
                DarkThemeConfig.LIGHT -> false
                DarkThemeConfig.DARK -> true
            }
    }

    /**
     * Returns `true` if the state wasn't loaded yet and it should keep showing the splash screen.
     */
    fun shouldKeepSplashScreen() = this is Loading

    /**
     * Returns `true` if the first-launch welcome screen should be shown.
     */
    val shouldShowWelcome: Boolean get() = false

    /**
     * Returns `true` if the dynamic color is disabled.
     */
    val shouldDisableDynamicTheming: Boolean get() = true

    /**
     * The custom theme seed color as a packed ARGB int, or 0 for the default scheme.
     */
    val seedColorArgb: Long get() = 0L

    /**
     * The vibrancy style used when generating a scheme from [seedColorArgb].
     */
    val themeStyle: ThemeStyle get() = ThemeStyle.TONAL_SPOT

    /**
     * The minimum contrast enforced on the generated custom scheme.
     */
    val themeContrast: ThemeContrast get() = ThemeContrast.STANDARD

    /**
     * Whether the user has enabled the decorative mesh gradient (runtime gates applied separately).
     */
    val useMeshGradient: Boolean get() = false

    /**
     * The chosen named HeroUI palette for the static scheme.
     */
    val themePalette: ThemePalette get() = ThemePalette.DEFAULT

    /**
     * Returns `true` if dark theme should be used.
     */
    fun shouldUseDarkTheme(isSystemDarkTheme: Boolean) = isSystemDarkTheme
}
