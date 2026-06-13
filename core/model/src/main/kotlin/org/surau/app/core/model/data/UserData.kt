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

package org.surau.app.core.model.data

import org.surau.app.core.model.data.quran.ReaderMode

/**
 * Locally stored user settings: app appearance plus Quran reader preferences.
 *
 * Reader preferences mirror the backend's user preferences for signed-in users; for guests they
 * are local-only.
 */
data class UserData(
    val darkThemeConfig: DarkThemeConfig,
    val useDynamicColor: Boolean,
    val readerMode: ReaderMode,
    val translationSourceId: String?,
    val recitationId: String?,
    val arabicFontScale: Float,
    val welcomeShown: Boolean,
    val flowArabicFontScale: Float = DEFAULT_ARABIC_FONT_SCALE,
    val flowShowTranslation: Boolean = false,
) {
    companion object {
        const val DEFAULT_ARABIC_FONT_SCALE = 1f
    }
}
