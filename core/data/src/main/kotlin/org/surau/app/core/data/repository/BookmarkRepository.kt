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

package org.surau.app.core.data.repository

import kotlinx.coroutines.flow.Flow
import org.surau.app.core.model.data.quran.AyahKey
import org.surau.app.core.model.data.quran.Bookmark

/**
 * The user's saved Quran ayat ("bookmarks"). Local-first: every change lands in Room immediately
 * (guest included); for signed-in users changes are pushed best-effort and reconciled with the
 * backend (`/me/saved-items`) newest-wins. One bookmark per ayah (keyed by [AyahKey]).
 */
interface BookmarkRepository {

    /** All saved ayat, grouped by surah then ayah, with pending deletions hidden. */
    fun observeBookmarks(): Flow<List<Bookmark>>

    /** The bookmark for [ayahKey], or `null` — drives the reader's per-ayah toggle. */
    fun observeBookmark(ayahKey: AyahKey): Flow<Bookmark?>

    /** The distinct tags across all live bookmarks, sorted — for the filter chips. */
    fun observeTags(): Flow<List<String>>

    /**
     * Saves [ayahKey] now (optionally with a [note] / [tags]). Re-adding a previously removed ayah
     * revives it. Marks it pending sync and best-effort publishes it for signed-in users.
     */
    suspend fun addBookmark(ayahKey: AyahKey, note: String? = null, tags: List<String> = emptyList())

    /**
     * Replaces the [note] and [tags] of an existing bookmark (the editor's "save"). A `null` note
     * or empty [tags] clears that field on the backend. No-op if [ayahKey] is not bookmarked.
     */
    suspend fun updateBookmark(ayahKey: AyahKey, note: String?, tags: List<String>)

    /** Removes [ayahKey]'s bookmark (tombstoned for signed-in users until the backend confirms). */
    suspend fun removeBookmark(ayahKey: AyahKey)

    /**
     * Pushes pending local creates/edits/deletes to the backend if signed in. Safe to call when
     * there is nothing to push. Never throws — failures keep the change pending.
     */
    suspend fun pushPending()

    /**
     * Reconciles local and remote bookmarks (newest `updated_at` wins; adopts server IDs; deletes
     * locally what was removed server-side). Called after login and by the background sync worker.
     */
    suspend fun reconcile()

    /**
     * Deletes every locally stored bookmark. Called on sign-out / account deletion so the next user
     * on this device can neither read the previous account's notes & tags nor have them re-pushed to
     * their own account on login. Already-synced bookmarks are safe — they are re-pulled from the
     * server on the next sign-in.
     */
    suspend fun clearLocalData()
}
