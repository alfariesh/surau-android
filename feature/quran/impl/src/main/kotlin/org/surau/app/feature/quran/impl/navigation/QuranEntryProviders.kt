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

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.surau.app.core.navigation.Navigator
import org.surau.app.feature.quran.api.navigation.QuranBookmarksNavKey
import org.surau.app.feature.quran.api.navigation.QuranHomeNavKey
import org.surau.app.feature.quran.api.navigation.QuranSearchNavKey
import org.surau.app.feature.quran.api.navigation.SurahReaderNavKey
import org.surau.app.feature.quran.api.navigation.navigateToQuranBookmarks
import org.surau.app.feature.quran.api.navigation.navigateToQuranSearch
import org.surau.app.feature.quran.api.navigation.navigateToSurahReader
import org.surau.app.feature.quran.impl.QuranBookmarksScreen
import org.surau.app.feature.quran.impl.QuranHomeScreen
import org.surau.app.feature.quran.impl.QuranSearchScreen
import org.surau.app.feature.quran.impl.SurahReaderPlaceholder
import org.surau.app.feature.quran.impl.SurahReaderScreen

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.quranHomeEntry(
    navigator: Navigator,
    onSettingsClick: () -> Unit,
) {
    // On expanded widths this pairs with the SurahReader detail pane (two-pane list-detail). On
    // compact widths the strategy collapses to a single pane, so phones are unaffected.
    entry<QuranHomeNavKey>(
        metadata = ListDetailSceneStrategy.listPane { SurahReaderPlaceholder() },
    ) {
        QuranHomeScreen(
            onSurahClick = { surahId, ayahNumber ->
                navigator.navigateToSurahReader(surahId, ayahNumber)
            },
            onSearchClick = navigator::navigateToQuranSearch,
            onBookmarksClick = navigator::navigateToQuranBookmarks,
            onSettingsClick = onSettingsClick,
        )
    }
}

fun EntryProviderScope<NavKey>.quranBookmarksEntry(navigator: Navigator) {
    entry<QuranBookmarksNavKey> {
        QuranBookmarksScreen(
            onBackClick = navigator::goBack,
            onOpenInReader = { surahId, ayahNumber ->
                navigator.navigateToSurahReader(surahId, ayahNumber)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.surahReaderEntry(
    navigator: Navigator,
    onFlowClick: (surahId: Int, ayahNumber: Int?) -> Unit,
) {
    entry<SurahReaderNavKey>(metadata = ListDetailSceneStrategy.detailPane()) { navKey ->
        SurahReaderScreen(
            navKey = navKey,
            onBackClick = navigator::goBack,
            onFlowClick = { ayahNumber -> onFlowClick(navKey.surahId, ayahNumber) },
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
