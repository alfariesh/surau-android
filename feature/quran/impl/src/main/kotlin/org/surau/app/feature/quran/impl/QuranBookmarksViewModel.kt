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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.surau.app.core.data.repository.BookmarkRepository
import org.surau.app.core.data.repository.QuranRepository
import org.surau.app.core.model.data.quran.AyahKey
import org.surau.app.core.model.data.quran.Bookmark
import javax.inject.Inject

@HiltViewModel
class QuranBookmarksViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository,
    private val quranRepository: QuranRepository,
) : ViewModel() {

    private val activeTag = MutableStateFlow<String?>(null)

    /**
     * Arabic text (keyed by ayah key) for the surahs that have bookmarks, read from the cache only —
     * a bookmarked surah is usually already cached from reading it. Missing entries degrade to no
     * preview rather than forcing a network fetch, keeping the screen fast and offline-robust.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val ayahTexts: Flow<Map<String, String>> =
        bookmarkRepository.observeBookmarks()
            .map { bookmarks -> bookmarks.map(Bookmark::surahId).distinct().sorted() }
            .distinctUntilChanged()
            .flatMapLatest { surahIds ->
                if (surahIds.isEmpty()) {
                    flowOf(emptyMap())
                } else {
                    val sourceId = quranRepository.resolveTranslationSourceId(null)
                    combine(surahIds.map { quranRepository.observeAyahs(it, sourceId) }) { lists ->
                        lists.asSequence().flatten()
                            .associate { it.ayah.ayahKey.value to it.ayah.textQpcHafs }
                    }
                }
            }

    val uiState: StateFlow<BookmarksUiState> =
        combine(
            bookmarkRepository.observeBookmarks(),
            bookmarkRepository.observeTags(),
            quranRepository.observeSurahs(),
            activeTag,
            ayahTexts,
        ) { bookmarks, tags, surahs, tag, texts ->
            if (bookmarks.isEmpty()) {
                BookmarksUiState.Empty
            } else {
                val surahNames = surahs.associate { it.surahId to it.nameLatin }
                val filtered = if (tag == null) bookmarks else bookmarks.filter { tag in it.tags }
                val sections = filtered
                    .groupBy { it.surahId }
                    .toSortedMap()
                    .map { (surahId, items) ->
                        BookmarkSurahSection(
                            surahId = surahId,
                            surahName = surahNames[surahId] ?: "Surah $surahId",
                            items = items.sortedBy { it.ayahNumber }
                                .map { it.toListItem(surahNames[it.surahId], texts[it.ayahKey.value]) },
                        )
                    }
                BookmarksUiState.Success(
                    sections = sections,
                    allTags = tags,
                    activeTag = tag,
                )
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = BookmarksUiState.Loading,
            )

    fun setTagFilter(tag: String?) {
        activeTag.value = tag
    }

    fun updateBookmark(ayahKey: AyahKey, note: String?, tags: List<String>) {
        viewModelScope.launch {
            bookmarkRepository.updateBookmark(ayahKey, note?.ifBlank { null }, tags)
        }
    }

    fun removeBookmark(ayahKey: AyahKey) {
        viewModelScope.launch {
            bookmarkRepository.removeBookmark(ayahKey)
        }
    }
}

private fun Bookmark.toListItem(surahName: String?, arabicText: String?) = BookmarkListItem(
    ayahKey = ayahKey,
    surahId = surahId,
    ayahNumber = ayahNumber,
    surahName = surahName ?: "Surah $surahId",
    arabicText = arabicText,
    note = note,
    tags = tags,
)

data class BookmarkListItem(
    val ayahKey: AyahKey,
    val surahId: Int,
    val ayahNumber: Int,
    val surahName: String,
    val arabicText: String?,
    val note: String?,
    val tags: List<String>,
)

data class BookmarkSurahSection(
    val surahId: Int,
    val surahName: String,
    val items: List<BookmarkListItem>,
)

sealed interface BookmarksUiState {
    data object Loading : BookmarksUiState

    data object Empty : BookmarksUiState

    data class Success(
        val sections: List<BookmarkSurahSection>,
        val allTags: List<String>,
        val activeTag: String?,
    ) : BookmarksUiState
}
