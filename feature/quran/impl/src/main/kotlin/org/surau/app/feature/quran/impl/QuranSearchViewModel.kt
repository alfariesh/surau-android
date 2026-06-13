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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import org.surau.app.core.data.repository.QuranRepository
import org.surau.app.core.data.repository.QuranSearchResult
import javax.inject.Inject

@HiltViewModel
class QuranSearchViewModel @Inject constructor(
    private val quranRepository: QuranRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val query: StateFlow<String> = savedStateHandle.getStateFlow(QUERY_KEY, "")

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val uiState: StateFlow<QuranSearchUiState> =
        query
            .debounce(QUERY_DEBOUNCE_MS)
            .distinctUntilChanged()
            .flatMapLatest { query ->
                flow {
                    if (query.trim().length < MIN_QUERY_LENGTH) {
                        emit(QuranSearchUiState.EmptyQuery)
                    } else {
                        emit(QuranSearchUiState.Loading)
                        emit(
                            try {
                                QuranSearchUiState.Success(
                                    query = query,
                                    results = quranRepository.search(query.trim()),
                                )
                            } catch (exception: Exception) {
                                QuranSearchUiState.Error
                            },
                        )
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = QuranSearchUiState.EmptyQuery,
            )

    fun onQueryChanged(query: String) {
        savedStateHandle[QUERY_KEY] = query
    }

    companion object {
        private const val QUERY_KEY = "query"
        private const val QUERY_DEBOUNCE_MS = 300L
        const val MIN_QUERY_LENGTH = 3
    }
}

sealed interface QuranSearchUiState {
    data object EmptyQuery : QuranSearchUiState

    data object Loading : QuranSearchUiState

    data object Error : QuranSearchUiState

    data class Success(
        val query: String,
        val results: List<QuranSearchResult>,
    ) : QuranSearchUiState {
        val isEmpty: Boolean get() = results.isEmpty()
    }
}
