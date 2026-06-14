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

package org.surau.app.core.data.test.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import org.surau.app.core.data.repository.BookmarkRepository
import org.surau.app.core.model.data.quran.AyahKey
import org.surau.app.core.model.data.quran.Bookmark
import javax.inject.Inject

/**
 * Fake [BookmarkRepository] holding bookmarks in memory (always "synced").
 */
class FakeBookmarkRepository @Inject constructor() : BookmarkRepository {

    private val bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())

    fun setBookmarks(value: List<Bookmark>) {
        bookmarks.value = value.sortedWith(compareBy({ it.surahId }, { it.ayahNumber }))
    }

    override fun observeBookmarks(): Flow<List<Bookmark>> = bookmarks

    override fun observeBookmark(ayahKey: AyahKey): Flow<Bookmark?> =
        bookmarks.map { list -> list.firstOrNull { it.ayahKey == ayahKey } }

    override fun observeTags(): Flow<List<String>> =
        bookmarks.map { list -> list.flatMap { it.tags }.distinct().sorted() }

    override suspend fun addBookmark(ayahKey: AyahKey, note: String?, tags: List<String>) {
        val now = Clock.System.now()
        bookmarks.update { list ->
            val existing = list.firstOrNull { it.ayahKey == ayahKey }
            val updated = existing?.copy(
                note = note ?: existing.note,
                tags = tags.ifEmpty { existing.tags },
                updatedAt = now,
            ) ?: Bookmark(ayahKey = ayahKey, note = note, tags = tags, createdAt = now, updatedAt = now)
            (list.filterNot { it.ayahKey == ayahKey } + updated)
                .sortedWith(compareBy({ it.surahId }, { it.ayahNumber }))
        }
    }

    override suspend fun updateBookmark(ayahKey: AyahKey, note: String?, tags: List<String>) {
        bookmarks.update { list ->
            list.map {
                if (it.ayahKey == ayahKey) {
                    it.copy(note = note, tags = tags, updatedAt = Clock.System.now())
                } else {
                    it
                }
            }
        }
    }

    override suspend fun removeBookmark(ayahKey: AyahKey) {
        bookmarks.update { list -> list.filterNot { it.ayahKey == ayahKey } }
    }

    override suspend fun pushPending() = Unit

    override suspend fun reconcile() = Unit
}
