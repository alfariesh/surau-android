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

package org.surau.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import org.surau.app.core.database.model.BookmarkEntity

@Dao
interface BookmarkDao {

    /** All live bookmarks (tombstones hidden), grouped by surah then ayah for the list UI. */
    @Query("SELECT * FROM bookmarks WHERE pending_delete = 0 ORDER BY surah_id, ayah_key")
    fun observeBookmarks(): Flow<List<BookmarkEntity>>

    /** The live bookmark for [ayahKey], or null — drives the reader's per-ayah toggle. */
    @Query("SELECT * FROM bookmarks WHERE ayah_key = :ayahKey AND pending_delete = 0 LIMIT 1")
    fun observeByAyahKey(ayahKey: String): Flow<BookmarkEntity?>

    /** Any row (incl. tombstone) for [ayahKey]; used to reuse the row on re-add. */
    @Query("SELECT * FROM bookmarks WHERE ayah_key = :ayahKey LIMIT 1")
    suspend fun getByAyahKey(ayahKey: String): BookmarkEntity?

    @Query("SELECT * FROM bookmarks WHERE pending_sync = 1 AND pending_delete = 0")
    suspend fun pendingUpserts(): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE pending_delete = 1")
    suspend fun pendingDeletes(): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks")
    suspend fun all(): List<BookmarkEntity>

    @Upsert
    suspend fun upsert(bookmark: BookmarkEntity)

    @Query("UPDATE bookmarks SET pending_delete = 1, updated_at = :updatedAt WHERE ayah_key = :ayahKey")
    suspend fun markPendingDelete(ayahKey: String, updatedAt: Instant)

    @Query("DELETE FROM bookmarks WHERE ayah_key = :ayahKey")
    suspend fun deleteByAyahKey(ayahKey: String)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM bookmarks")
    suspend fun clear()
}
