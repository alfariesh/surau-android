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

package org.surau.app.feature.quran.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.surau.app.feature.quran.api.navigation.SurahFlowNavKey

/**
 * The immersive Flow player embedded as the expanded face of the app-level expandable player. Unlike
 * the full-screen Flow destination, this never hijacks the window's system bars (so it can be
 * collapsed back to the mini-player) and its back affordance [onCollapse]s the sheet instead of
 * popping the navigation stack.
 *
 * The view-model is keyed on [surahId] only, so the same surah keeps one session as the sheet is
 * expanded/collapsed; [initialAyah] only seeds the first load. When [surahId] changes (auto-continue
 * or opening a new surah) a fresh session is created.
 */
@Composable
fun EmbeddedSurahFlowPlayer(
    surahId: Int,
    initialAyah: Int?,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navKey = remember(surahId, initialAyah) { SurahFlowNavKey(surahId, initialAyah) }
    SurahFlowScreen(
        navKey = navKey,
        onBackClick = onCollapse,
        embedded = true,
        modifier = modifier,
        viewModel = hiltViewModel<SurahFlowViewModel, SurahFlowViewModel.Factory>(
            key = "embedded-flow-$surahId",
        ) { factory ->
            factory.create(navKey)
        },
    )
}
