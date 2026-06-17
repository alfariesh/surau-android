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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.surau.app.core.model.data.DarkThemeConfig
import org.surau.app.core.model.data.ThemeContrast
import org.surau.app.core.model.data.ThemePalette
import org.surau.app.core.model.data.ThemeStyle
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
                flowAutoContinue = !it.flowAutoContinueDisabled,
                flowKeepScreenOn = !it.flowKeepScreenOnDisabled,
                readerShowTransliteration = it.readerShowTransliteration,
                readerShowTranslation = !it.readerTranslationHidden,
                readerArabicLineSpacing = if (it.readerArabicLineSpacingPercent <= 0) {
                    UserData.DEFAULT_LINE_SPACING
                } else {
                    it.readerArabicLineSpacingPercent / 100f
                },
                readerTranslationScale = if (it.readerTranslationScalePercent <= 0) {
                    UserData.DEFAULT_TRANSLATION_SCALE
                } else {
                    it.readerTranslationScalePercent / 100f
                },
                readerKeepScreenOn = !it.readerKeepScreenOnDisabled,
                seedColorArgb = it.seedColorArgb,
                themeStyle = when (it.themeStyle) {
                    null,
                    ThemeStyleProto.THEME_STYLE_UNSPECIFIED,
                    ThemeStyleProto.UNRECOGNIZED,
                    ThemeStyleProto.THEME_STYLE_TONAL_SPOT,
                    -> ThemeStyle.TONAL_SPOT
                    ThemeStyleProto.THEME_STYLE_VIBRANT -> ThemeStyle.VIBRANT
                    ThemeStyleProto.THEME_STYLE_EXPRESSIVE -> ThemeStyle.EXPRESSIVE
                    ThemeStyleProto.THEME_STYLE_NEUTRAL -> ThemeStyle.NEUTRAL
                },
                themeContrast = when (it.themeContrast) {
                    null,
                    ThemeContrastProto.THEME_CONTRAST_UNSPECIFIED,
                    ThemeContrastProto.UNRECOGNIZED,
                    ThemeContrastProto.THEME_CONTRAST_STANDARD,
                    -> ThemeContrast.STANDARD
                    ThemeContrastProto.THEME_CONTRAST_MEDIUM -> ThemeContrast.MEDIUM
                    ThemeContrastProto.THEME_CONTRAST_HIGH -> ThemeContrast.HIGH
                },
                useMeshGradient = it.useMeshGradient,
                themePalette = when (it.themePalette) {
                    null,
                    ThemePaletteProto.THEME_PALETTE_UNSPECIFIED,
                    ThemePaletteProto.UNRECOGNIZED,
                    ThemePaletteProto.THEME_PALETTE_SURAU_BASE,
                    -> ThemePalette.SURAU_BASE
                    ThemePaletteProto.THEME_PALETTE_DEFAULT -> ThemePalette.DEFAULT
                    ThemePaletteProto.THEME_PALETTE_MOUVE -> ThemePalette.MOUVE
                    ThemePaletteProto.THEME_PALETTE_SKY -> ThemePalette.SKY
                    ThemePaletteProto.THEME_PALETTE_MINT -> ThemePalette.MINT
                    ThemePaletteProto.THEME_PALETTE_DISCORD -> ThemePalette.DISCORD
                    ThemePaletteProto.THEME_PALETTE_UBER -> ThemePalette.UBER
                    ThemePaletteProto.THEME_PALETTE_AIRBNB -> ThemePalette.AIRBNB
                },
            )
        }
        // Coalesce duplicate emissions: DataStore re-emits the whole proto on every write, so an
        // unrelated preference change should not wake every per-field collector.
        .distinctUntilChanged()

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

    suspend fun setFlowAutoContinue(enabled: Boolean) {
        userPreferences.updateData {
            it.copy { this.flowAutoContinueDisabled = !enabled }
        }
    }

    suspend fun setFlowKeepScreenOn(enabled: Boolean) {
        userPreferences.updateData {
            it.copy { this.flowKeepScreenOnDisabled = !enabled }
        }
    }

    suspend fun setReaderShowTransliteration(show: Boolean) {
        userPreferences.updateData {
            it.copy { this.readerShowTransliteration = show }
        }
    }

    suspend fun setReaderShowTranslation(show: Boolean) {
        userPreferences.updateData {
            it.copy { this.readerTranslationHidden = !show }
        }
    }

    suspend fun setReaderArabicLineSpacing(spacing: Float) {
        userPreferences.updateData {
            it.copy { this.readerArabicLineSpacingPercent = (spacing * 100).toInt() }
        }
    }

    suspend fun setReaderTranslationScale(scale: Float) {
        userPreferences.updateData {
            it.copy { this.readerTranslationScalePercent = (scale * 100).toInt() }
        }
    }

    suspend fun setReaderKeepScreenOn(enabled: Boolean) {
        userPreferences.updateData {
            it.copy { this.readerKeepScreenOnDisabled = !enabled }
        }
    }

    suspend fun setWelcomeShown(shown: Boolean) {
        userPreferences.updateData {
            it.copy { this.welcomeShown = shown }
        }
    }

    /** Stores the custom theme seed (ARGB). Alpha is forced to 0xFF so 0 reliably means "unset". */
    suspend fun setSeedColor(argb: Long) {
        val normalized = if (argb == 0L) 0L else argb or 0xFF000000L
        userPreferences.updateData {
            it.copy { this.seedColorArgb = normalized }
        }
    }

    suspend fun setThemeStyle(themeStyle: ThemeStyle) {
        userPreferences.updateData {
            it.copy {
                this.themeStyle = when (themeStyle) {
                    ThemeStyle.TONAL_SPOT -> ThemeStyleProto.THEME_STYLE_TONAL_SPOT
                    ThemeStyle.VIBRANT -> ThemeStyleProto.THEME_STYLE_VIBRANT
                    ThemeStyle.EXPRESSIVE -> ThemeStyleProto.THEME_STYLE_EXPRESSIVE
                    ThemeStyle.NEUTRAL -> ThemeStyleProto.THEME_STYLE_NEUTRAL
                }
            }
        }
    }

    suspend fun setThemeContrast(themeContrast: ThemeContrast) {
        userPreferences.updateData {
            it.copy {
                this.themeContrast = when (themeContrast) {
                    ThemeContrast.STANDARD -> ThemeContrastProto.THEME_CONTRAST_STANDARD
                    ThemeContrast.MEDIUM -> ThemeContrastProto.THEME_CONTRAST_MEDIUM
                    ThemeContrast.HIGH -> ThemeContrastProto.THEME_CONTRAST_HIGH
                }
            }
        }
    }

    suspend fun setMeshGradientPreference(useMeshGradient: Boolean) {
        userPreferences.updateData {
            it.copy { this.useMeshGradient = useMeshGradient }
        }
    }

    suspend fun setThemePalette(themePalette: ThemePalette) {
        userPreferences.updateData {
            it.copy {
                this.themePalette = when (themePalette) {
                    ThemePalette.SURAU_BASE -> ThemePaletteProto.THEME_PALETTE_SURAU_BASE
                    ThemePalette.DEFAULT -> ThemePaletteProto.THEME_PALETTE_DEFAULT
                    ThemePalette.MOUVE -> ThemePaletteProto.THEME_PALETTE_MOUVE
                    ThemePalette.SKY -> ThemePaletteProto.THEME_PALETTE_SKY
                    ThemePalette.MINT -> ThemePaletteProto.THEME_PALETTE_MINT
                    ThemePalette.DISCORD -> ThemePaletteProto.THEME_PALETTE_DISCORD
                    ThemePalette.UBER -> ThemePaletteProto.THEME_PALETTE_UBER
                    ThemePalette.AIRBNB -> ThemePaletteProto.THEME_PALETTE_AIRBNB
                }
            }
        }
    }
}
