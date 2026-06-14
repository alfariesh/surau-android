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

import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test
import org.surau.app.core.data.test.repository.FakeBookmarkRepository
import org.surau.app.core.data.test.repository.FakeQuranRepository
import org.surau.app.core.model.data.quran.AyahKey
import org.surau.app.core.model.data.quran.Bookmark
import org.surau.app.core.testing.util.MainDispatcherRule
import kotlin.test.assertEquals
import kotlin.test.assertIs

class QuranBookmarksViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val bookmarkRepository = FakeBookmarkRepository()
    private val quranRepository = FakeQuranRepository()

    private fun viewModel() = QuranBookmarksViewModel(bookmarkRepository, quranRepository)

    private suspend fun TurbineTestContext<BookmarksUiState>.awaitSuccess(): BookmarksUiState.Success {
        var item = awaitItem()
        while (item !is BookmarksUiState.Success) item = awaitItem()
        return item
    }

    @Test
    fun noBookmarks_isEmpty() = runTest {
        viewModel().uiState.test {
            var item = awaitItem()
            while (item is BookmarksUiState.Loading) item = awaitItem()
            assertIs<BookmarksUiState.Empty>(item)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun groupsBySurah_sortedByAyah_withResolvedNames() = runTest {
        bookmarkRepository.setBookmarks(
            listOf(bookmark("1:5"), bookmark("73:4"), bookmark("1:1")),
        )

        viewModel().uiState.test {
            val success = awaitSuccess()
            assertEquals(2, success.sections.size)
            assertEquals("Al-Fatihah", success.sections[0].surahName)
            assertEquals(listOf(1, 5), success.sections[0].items.map { it.ayahNumber })
            assertEquals("Al-Muzzammil", success.sections[1].surahName)
            assertEquals(listOf(4), success.sections[1].items.map { it.ayahNumber })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun tagFilter_restrictsToMatchingBookmarks() = runTest {
        bookmarkRepository.setBookmarks(
            listOf(
                bookmark("1:1", tags = listOf("tafsir")),
                bookmark("73:4", tags = listOf("favorit")),
            ),
        )
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitSuccess() // initial (unfiltered)
            viewModel.setTagFilter("favorit")
            val filtered = awaitSuccess()
            assertEquals(1, filtered.sections.size)
            assertEquals(73, filtered.sections.single().surahId)
            assertEquals("favorit", filtered.activeTag)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun removeBookmark_delegatesToRepository() = runTest {
        bookmarkRepository.setBookmarks(listOf(bookmark("1:1"), bookmark("73:4")))

        viewModel().removeBookmark(AyahKey("1:1"))
        runCurrent()

        assertEquals(listOf("73:4"), bookmarkRepository.observeBookmarks().first().map { it.ayahKey.value })
    }

    private fun bookmark(
        key: String,
        note: String? = null,
        tags: List<String> = emptyList(),
    ) = Bookmark(
        ayahKey = AyahKey(key),
        note = note,
        tags = tags,
        createdAt = Instant.fromEpochSeconds(1),
        updatedAt = Instant.fromEpochSeconds(1),
    )
}
