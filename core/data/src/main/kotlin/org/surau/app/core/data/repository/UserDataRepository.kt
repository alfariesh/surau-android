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

package org.surau.app.core.data.repository

import kotlinx.coroutines.flow.Flow
import org.surau.app.core.model.data.DarkThemeConfig
import org.surau.app.core.model.data.UserData
import org.surau.app.core.model.data.quran.ReaderMode

interface UserDataRepository {

    /**
     * Stream of [UserData]
     */
    val userData: Flow<UserData>

    /**
     * Sets the desired dark theme config.
     */
    suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig)

    /**
     * Sets the preferred dynamic color config.
     */
    suspend fun setDynamicColorPreference(useDynamicColor: Boolean)

    /**
     * Sets how the Quran reader lays out ayahs.
     */
    suspend fun setReaderMode(readerMode: ReaderMode)

    /**
     * Sets the preferred Quran translation source, or `null` to use the backend default.
     */
    suspend fun setTranslationSourceId(translationSourceId: String?)

    /**
     * Sets the preferred recitation (milestone 2), or `null` to use the backend default.
     */
    suspend fun setRecitationId(recitationId: String?)

    /**
     * Sets the Arabic font scale (1.0 = default).
     */
    suspend fun setArabicFontScale(scale: Float)

    /**
     * Sets the immersive Flow reader's Arabic font scale (1.0 = default). Local-only.
     */
    suspend fun setFlowArabicFontScale(scale: Float)

    /**
     * Sets whether the immersive Flow reader shows the translation. Local-only.
     */
    suspend fun setFlowShowTranslation(show: Boolean)

    /**
     * Sets whether Flow auto-continues to the next surah when one finishes. Local-only.
     */
    suspend fun setFlowAutoContinue(enabled: Boolean)

    /**
     * Sets whether Flow keeps the screen on while a recitation is playing. Local-only.
     */
    suspend fun setFlowKeepScreenOn(enabled: Boolean)

    /**
     * Marks the first-launch welcome screen as shown.
     */
    suspend fun setWelcomeShown(shown: Boolean)
}
