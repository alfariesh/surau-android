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

package org.surau.app.core.data.test.repository

import kotlinx.coroutines.flow.Flow
import org.surau.app.core.data.repository.UserDataRepository
import org.surau.app.core.datastore.SurauPreferencesDataSource
import org.surau.app.core.model.data.DarkThemeConfig
import org.surau.app.core.model.data.ThemeContrast
import org.surau.app.core.model.data.ThemePalette
import org.surau.app.core.model.data.ThemeStyle
import org.surau.app.core.model.data.UserData
import org.surau.app.core.model.data.quran.ReaderMode
import javax.inject.Inject

/**
 * Fake implementation of the [UserDataRepository] backed directly by the preferences DataStore,
 * with no analytics side effects.
 */
class FakeUserDataRepository @Inject constructor(
    private val surauPreferencesDataSource: SurauPreferencesDataSource,
) : UserDataRepository {

    override val userData: Flow<UserData> =
        surauPreferencesDataSource.userData

    override suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig) {
        surauPreferencesDataSource.setDarkThemeConfig(darkThemeConfig)
    }

    override suspend fun setDynamicColorPreference(useDynamicColor: Boolean) {
        surauPreferencesDataSource.setDynamicColorPreference(useDynamicColor)
    }

    override suspend fun setSeedColor(argb: Long) {
        surauPreferencesDataSource.setSeedColor(argb)
    }

    override suspend fun setThemeStyle(themeStyle: ThemeStyle) {
        surauPreferencesDataSource.setThemeStyle(themeStyle)
    }

    override suspend fun setThemeContrast(themeContrast: ThemeContrast) {
        surauPreferencesDataSource.setThemeContrast(themeContrast)
    }

    override suspend fun setMeshGradientPreference(useMeshGradient: Boolean) {
        surauPreferencesDataSource.setMeshGradientPreference(useMeshGradient)
    }

    override suspend fun setThemePalette(themePalette: ThemePalette) {
        surauPreferencesDataSource.setThemePalette(themePalette)
    }

    override suspend fun setReaderMode(readerMode: ReaderMode) {
        surauPreferencesDataSource.setReaderMode(readerMode)
    }

    override suspend fun setTranslationSourceId(translationSourceId: String?) {
        surauPreferencesDataSource.setTranslationSourceId(translationSourceId)
    }

    override suspend fun setRecitationId(recitationId: String?) {
        surauPreferencesDataSource.setRecitationId(recitationId)
    }

    override suspend fun setArabicFontScale(scale: Float) {
        surauPreferencesDataSource.setArabicFontScale(scale)
    }

    override suspend fun setFlowArabicFontScale(scale: Float) {
        surauPreferencesDataSource.setFlowArabicFontScale(scale)
    }

    override suspend fun setFlowShowTranslation(show: Boolean) {
        surauPreferencesDataSource.setFlowShowTranslation(show)
    }

    override suspend fun setFlowAutoContinue(enabled: Boolean) {
        surauPreferencesDataSource.setFlowAutoContinue(enabled)
    }

    override suspend fun setFlowKeepScreenOn(enabled: Boolean) {
        surauPreferencesDataSource.setFlowKeepScreenOn(enabled)
    }

    override suspend fun setReaderShowTransliteration(show: Boolean) {
        surauPreferencesDataSource.setReaderShowTransliteration(show)
    }

    override suspend fun setReaderShowTranslation(show: Boolean) {
        surauPreferencesDataSource.setReaderShowTranslation(show)
    }

    override suspend fun setReaderArabicLineSpacing(spacing: Float) {
        surauPreferencesDataSource.setReaderArabicLineSpacing(spacing)
    }

    override suspend fun setReaderTranslationScale(scale: Float) {
        surauPreferencesDataSource.setReaderTranslationScale(scale)
    }

    override suspend fun setReaderKeepScreenOn(enabled: Boolean) {
        surauPreferencesDataSource.setReaderKeepScreenOn(enabled)
    }

    override suspend fun setWelcomeShown(shown: Boolean) {
        surauPreferencesDataSource.setWelcomeShown(shown)
    }
}
