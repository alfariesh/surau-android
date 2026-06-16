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

package org.surau.app.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.surau.app.core.media.PlayerUiState
import org.surau.app.core.media.SurauPlayerController
import javax.inject.Inject

/**
 * App-level bridge to the process-singleton [SurauPlayerController]. Exposes the playback snapshot
 * (which survives navigation and config changes) plus the controls the global mini-player needs, so
 * the expandable player can live in the app shell above the navigation host.
 */
@HiltViewModel
class AppPlayerViewModel @Inject constructor(
    private val controller: SurauPlayerController,
) : ViewModel() {

    val state: StateFlow<PlayerUiState> = controller.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerUiState())

    fun playPause() = controller.playPause()

    fun next() = controller.next()

    fun previous() = controller.previous()

    fun stop() = controller.stop()
}
