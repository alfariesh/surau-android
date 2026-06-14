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

package org.surau.app.feature.quran.api.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.surau.app.core.navigation.Navigator

/**
 * The Quran home (surah & juz lists). The app's start destination.
 */
@Serializable
object QuranHomeNavKey : NavKey

/**
 * The surah reader.
 *
 * @property surahId surah to open (1–114).
 * @property ayahNumber optional ayah to scroll to (resume / search result).
 */
@Serializable
data class SurahReaderNavKey(
    val surahId: Int,
    val ayahNumber: Int? = null,
) : NavKey

/**
 * The immersive "Flow" reader: surah-mode recitation with the active ayah centered and
 * auto-scrolling, for focused listening (tadabbur).
 *
 * @property surahId surah to open.
 * @property ayahNumber optional ayah to start from.
 */
@Serializable
data class SurahFlowNavKey(
    val surahId: Int,
    val ayahNumber: Int? = null,
) : NavKey

/**
 * Server-side Quran search.
 */
@Serializable
object QuranSearchNavKey : NavKey

/**
 * The user's saved ayat (bookmarks).
 */
@Serializable
object QuranBookmarksNavKey : NavKey

fun Navigator.navigateToSurahReader(surahId: Int, ayahNumber: Int? = null) {
    navigate(SurahReaderNavKey(surahId = surahId, ayahNumber = ayahNumber))
}

fun Navigator.navigateToSurahFlow(surahId: Int, ayahNumber: Int? = null) {
    navigate(SurahFlowNavKey(surahId = surahId, ayahNumber = ayahNumber))
}

fun Navigator.navigateToQuranSearch() {
    navigate(QuranSearchNavKey)
}

fun Navigator.navigateToQuranBookmarks() {
    navigate(QuranBookmarksNavKey)
}
