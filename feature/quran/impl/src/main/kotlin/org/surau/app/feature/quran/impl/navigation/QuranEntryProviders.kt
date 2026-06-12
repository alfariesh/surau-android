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

package org.surau.app.feature.quran.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.surau.app.core.navigation.Navigator
import org.surau.app.feature.quran.api.navigation.QuranHomeNavKey
import org.surau.app.feature.quran.api.navigation.QuranSearchNavKey
import org.surau.app.feature.quran.api.navigation.SurahReaderNavKey
import org.surau.app.feature.quran.api.navigation.navigateToQuranSearch
import org.surau.app.feature.quran.api.navigation.navigateToSurahReader
import org.surau.app.feature.quran.impl.QuranHomeScreen
import org.surau.app.feature.quran.impl.QuranSearchScreen
import org.surau.app.feature.quran.impl.SurahReaderScreen

fun EntryProviderScope<NavKey>.quranHomeEntry(
    navigator: Navigator,
    onSettingsClick: () -> Unit,
) {
    entry<QuranHomeNavKey> {
        QuranHomeScreen(
            onSurahClick = { surahId, ayahNumber ->
                navigator.navigateToSurahReader(surahId, ayahNumber)
            },
            onSearchClick = navigator::navigateToQuranSearch,
            onSettingsClick = onSettingsClick,
        )
    }
}

fun EntryProviderScope<NavKey>.surahReaderEntry(navigator: Navigator) {
    entry<SurahReaderNavKey> { navKey ->
        SurahReaderScreen(
            navKey = navKey,
            onBackClick = navigator::goBack,
        )
    }
}

fun EntryProviderScope<NavKey>.quranSearchEntry(navigator: Navigator) {
    entry<QuranSearchNavKey> {
        QuranSearchScreen(
            onBackClick = navigator::goBack,
            onResultClick = { surahId, ayahNumber ->
                navigator.navigateToSurahReader(surahId, ayahNumber)
            },
        )
    }
}
