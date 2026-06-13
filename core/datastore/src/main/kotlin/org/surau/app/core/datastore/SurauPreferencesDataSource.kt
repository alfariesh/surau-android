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

package org.surau.app.core.datastore

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.map
import org.surau.app.core.model.data.DarkThemeConfig
import org.surau.app.core.model.data.UserData
import org.surau.app.core.model.data.quran.ReaderMode
import javax.inject.Inject

class SurauPreferencesDataSource @Inject constructor(
    private val userPreferences: DataStore<UserPreferences>,
) {
    val userData = userPreferences.data
        .map {
            UserData(
                darkThemeConfig = when (it.darkThemeConfig) {
                    null,
                    DarkThemeConfigProto.DARK_THEME_CONFIG_UNSPECIFIED,
                    DarkThemeConfigProto.UNRECOGNIZED,
                    DarkThemeConfigProto.DARK_THEME_CONFIG_FOLLOW_SYSTEM,
                    ->
                        DarkThemeConfig.FOLLOW_SYSTEM
                    DarkThemeConfigProto.DARK_THEME_CONFIG_LIGHT ->
                        DarkThemeConfig.LIGHT
                    DarkThemeConfigProto.DARK_THEME_CONFIG_DARK -> DarkThemeConfig.DARK
                },
                useDynamicColor = it.useDynamicColor,
                readerMode = when (it.readerMode) {
                    null,
                    ReaderModeProto.READER_MODE_UNSPECIFIED,
                    ReaderModeProto.UNRECOGNIZED,
                    ReaderModeProto.READER_MODE_ARABIC_TRANSLATION,
                    -> ReaderMode.ARABIC_TRANSLATION
                    ReaderModeProto.READER_MODE_TRANSLATION_ONLY -> ReaderMode.TRANSLATION_ONLY
                    ReaderModeProto.READER_MODE_ARABIC_ONLY -> ReaderMode.ARABIC_ONLY
                },
                translationSourceId = it.translationSourceId.ifEmpty { null },
                recitationId = it.recitationId.ifEmpty { null },
                arabicFontScale = if (it.arabicFontScalePercent <= 0) {
                    UserData.DEFAULT_ARABIC_FONT_SCALE
                } else {
                    it.arabicFontScalePercent / 100f
                },
                welcomeShown = it.welcomeShown,
                flowArabicFontScale = if (it.flowArabicFontScalePercent <= 0) {
                    UserData.DEFAULT_ARABIC_FONT_SCALE
                } else {
                    it.flowArabicFontScalePercent / 100f
                },
                flowShowTranslation = it.flowShowTranslation,
            )
        }

    suspend fun setDynamicColorPreference(useDynamicColor: Boolean) {
        userPreferences.updateData {
            it.copy { this.useDynamicColor = useDynamicColor }
        }
    }

    suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig) {
        userPreferences.updateData {
            it.copy {
                this.darkThemeConfig = when (darkThemeConfig) {
                    DarkThemeConfig.FOLLOW_SYSTEM ->
                        DarkThemeConfigProto.DARK_THEME_CONFIG_FOLLOW_SYSTEM
                    DarkThemeConfig.LIGHT -> DarkThemeConfigProto.DARK_THEME_CONFIG_LIGHT
                    DarkThemeConfig.DARK -> DarkThemeConfigProto.DARK_THEME_CONFIG_DARK
                }
            }
        }
    }

    suspend fun setReaderMode(readerMode: ReaderMode) {
        userPreferences.updateData {
            it.copy {
                this.readerMode = when (readerMode) {
                    ReaderMode.ARABIC_TRANSLATION -> ReaderModeProto.READER_MODE_ARABIC_TRANSLATION
                    ReaderMode.TRANSLATION_ONLY -> ReaderModeProto.READER_MODE_TRANSLATION_ONLY
                    ReaderMode.ARABIC_ONLY -> ReaderModeProto.READER_MODE_ARABIC_ONLY
                }
            }
        }
    }

    suspend fun setTranslationSourceId(translationSourceId: String?) {
        userPreferences.updateData {
            it.copy { this.translationSourceId = translationSourceId.orEmpty() }
        }
    }

    suspend fun setRecitationId(recitationId: String?) {
        userPreferences.updateData {
            it.copy { this.recitationId = recitationId.orEmpty() }
        }
    }

    suspend fun setArabicFontScale(scale: Float) {
        userPreferences.updateData {
            it.copy { this.arabicFontScalePercent = (scale * 100).toInt() }
        }
    }

    suspend fun setFlowArabicFontScale(scale: Float) {
        userPreferences.updateData {
            it.copy { this.flowArabicFontScalePercent = (scale * 100).toInt() }
        }
    }

    suspend fun setFlowShowTranslation(show: Boolean) {
        userPreferences.updateData {
            it.copy { this.flowShowTranslation = show }
        }
    }

    suspend fun setWelcomeShown(shown: Boolean) {
        userPreferences.updateData {
            it.copy { this.welcomeShown = shown }
        }
    }
}
