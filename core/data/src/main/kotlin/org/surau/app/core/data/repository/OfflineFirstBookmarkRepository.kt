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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import org.surau.app.core.common.coroutines.runCatchingExceptCancellation
import org.surau.app.core.database.dao.BookmarkDao
import org.surau.app.core.database.model.BookmarkEntity
import org.surau.app.core.database.model.asExternalModel
import org.surau.app.core.datastore.AuthSessionDataSource
import org.surau.app.core.model.data.auth.AuthState
import org.surau.app.core.model.data.quran.AyahKey
import org.surau.app.core.model.data.quran.Bookmark
import org.surau.app.core.network.model.SurauApiException
import org.surau.app.core.network.model.apiCall
import org.surau.app.core.network.model.me.CreateSavedItemRequestDto
import org.surau.app.core.network.model.me.PatchSavedItemRequestDto
import org.surau.app.core.network.model.me.SavedItemDto
import org.surau.app.core.network.retrofit.SurauMeApi
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

internal class OfflineFirstBookmarkRepository @Inject constructor(
    private val bookmarkDao: BookmarkDao,
    private val meApi: SurauMeApi,
    private val authSessionDataSource: AuthSessionDataSource,
) : BookmarkRepository {

    // Serialises the network sync sections so a background reconcile and a user-triggered push (or
    // two pushes) can't run concurrently and double-POST. Local Room writes stay outside the lock so
    // bookmarking always lands instantly, offline-first.
    private val syncMutex = Mutex()

    override fun observeBookmarks(): Flow<List<Bookmark>> =
        bookmarkDao.observeBookmarks().map { rows -> rows.map(BookmarkEntity::asExternalModel) }

    override fun observeBookmark(ayahKey: AyahKey): Flow<Bookmark?> =
        bookmarkDao.observeByAyahKey(ayahKey.value).map { it?.asExternalModel() }

    override fun observeTags(): Flow<List<String>> =
        bookmarkDao.observeBookmarks().map { rows ->
            rows.flatMap { it.tags }.distinct().sorted()
        }

    override suspend fun addBookmark(ayahKey: AyahKey, note: String?, tags: List<String>) {
        val existing = bookmarkDao.getByAyahKey(ayahKey.value)
        val now = Clock.System.now()
        val cleanNote = sanitizeNote(note)
        val cleanTags = sanitizeTags(tags)
        bookmarkDao.upsert(
            BookmarkEntity(
                id = existing?.id ?: 0,
                ayahKey = ayahKey.value,
                surahId = ayahKey.surahId,
                label = existing?.label,
                note = cleanNote ?: existing?.note,
                tags = cleanTags.ifEmpty { existing?.tags.orEmpty() },
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                // Keep any server id (revives an edited/tombstoned row) so we PATCH, not duplicate.
                serverId = existing?.serverId,
                pendingSync = true,
                pendingDelete = false,
            ),
        )
        pushPending()
    }

    override suspend fun updateBookmark(ayahKey: AyahKey, note: String?, tags: List<String>) {
        val existing = bookmarkDao.getByAyahKey(ayahKey.value) ?: return
        bookmarkDao.upsert(
            existing.copy(
                note = sanitizeNote(note),
                tags = sanitizeTags(tags),
                updatedAt = Clock.System.now(),
                pendingSync = true,
                pendingDelete = false,
            ),
        )
        pushPending()
    }

    override suspend fun removeBookmark(ayahKey: AyahKey) {
        val existing = bookmarkDao.getByAyahKey(ayahKey.value) ?: return
        if (existing.serverId == null) {
            // Never synced — nothing to delete server-side.
            bookmarkDao.deleteByAyahKey(ayahKey.value)
        } else {
            bookmarkDao.markPendingDelete(ayahKey.value, Clock.System.now())
        }
        pushPending()
    }

    override suspend fun pushPending() {
        if (!isAuthenticated()) return
        syncMutex.withLock { pushPendingLocked() }
    }

    override suspend fun clearLocalData() {
        // Under the sync lock so a wipe never races a concurrent reconcile/push leaving half a corpus.
        syncMutex.withLock { bookmarkDao.clear() }
    }

    private suspend fun pushPendingLocked() {
        pushPendingDeletes()
        pushPendingUpserts()
    }

    private suspend fun pushPendingDeletes() {
        for (tombstone in bookmarkDao.pendingDeletes()) {
            try {
                tombstone.serverId?.let { apiCall { meApi.deleteSavedItem(it) } }
                bookmarkDao.deleteById(tombstone.id)
            } catch (exception: SurauApiException) {
                // Already gone server-side — reconcile the local tombstone away too.
                if (exception.httpStatus == 404) bookmarkDao.deleteById(tombstone.id)
                // Otherwise keep the tombstone; the next push retries.
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // Connectivity — keep the tombstone.
            }
        }
    }

    private suspend fun pushPendingUpserts() {
        for (pending in bookmarkDao.pendingUpserts()) {
            try {
                if (pending.serverId == null) pushCreate(pending) else pushEdit(pending)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: SurauApiException) {
                // A validation rejection (400/422) will never succeed on retry — stop retrying so a
                // single malformed row can't wedge the sync queue forever. Other API errors
                // (401/403/429/5xx) stay pending and retry. Input is clamped on write, so this is a
                // defensive backstop rather than the primary guard.
                if (exception.httpStatus == 400 || exception.httpStatus == 422) {
                    bookmarkDao.getByAyahKey(pending.ayahKey)?.let {
                        if (it.id == pending.id) bookmarkDao.upsert(it.copy(pendingSync = false))
                    }
                }
            } catch (_: Exception) {
                // Keep pending; the sync worker or next push retries.
            }
        }
    }

    private suspend fun pushCreate(pending: BookmarkEntity) {
        val dto = apiCall {
            meApi.createSavedItem(
                CreateSavedItemRequestDto(
                    itemType = ITEM_TYPE_QURAN_AYAH,
                    surahId = pending.surahId,
                    ayahKey = pending.ayahKey,
                    note = pending.note,
                    tags = pending.tags.ifEmpty { null },
                ),
            )
        }
        when (val current = bookmarkDao.getByAyahKey(pending.ayahKey)) {
            null ->
                // Removed mid-flight — honor the deletion by undoing the server create.
                runCatchingExceptCancellation { apiCall { meApi.deleteSavedItem(dto.id) } }

            else -> {
                val unchanged = current.id == pending.id && current.updatedAt == pending.updatedAt
                if (unchanged && !current.pendingDelete) {
                    bookmarkDao.upsert(current.copy(serverId = dto.id, pendingSync = false))
                } else {
                    // Edited/tombstoned since the request — adopt the server id but keep it pending.
                    bookmarkDao.upsert(current.copy(serverId = dto.id))
                }
            }
        }
    }

    private suspend fun pushEdit(pending: BookmarkEntity) {
        val serverId = pending.serverId ?: return
        try {
            apiCall {
                meApi.patchSavedItem(
                    serverId,
                    PatchSavedItemRequestDto(note = pending.note, tags = pending.tags),
                )
            }
        } catch (exception: SurauApiException) {
            // Deleted server-side — drop the stale id so the next push re-creates it.
            if (exception.httpStatus == 404) {
                bookmarkDao.getByAyahKey(pending.ayahKey)?.let {
                    bookmarkDao.upsert(it.copy(serverId = null))
                }
                return
            }
            // Other API errors (incl. a terminal 400/422) bubble to pushPendingUpserts, which
            // decides whether to keep the row pending or stop retrying it.
            throw exception
        }
        val current = bookmarkDao.getByAyahKey(pending.ayahKey) ?: return
        if (current.id == pending.id && current.updatedAt == pending.updatedAt && !current.pendingDelete) {
            bookmarkDao.upsert(current.copy(pendingSync = false))
        }
    }

    override suspend fun reconcile() {
        if (!isAuthenticated()) return
        syncMutex.withLock {
            val remote = try {
                pullAllSavedItems()
            } catch (exception: SurauApiException) {
                if (exception.httpStatus == 404) emptyList() else return
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                return
            }

            val remoteByKey = remote.mapNotNull { dto -> dto.ayahKey?.let { it to dto } }.toMap()
            val local = bookmarkDao.all()
            val localByKey = local.associateBy { it.ayahKey }

            for ((key, dto) in remoteByKey) {
                mergeRemote(key, dto, localByKey[key])
            }

            // A previously-synced local bookmark missing from the remote set was deleted elsewhere.
            for (entity in local) {
                if (entity.serverId != null &&
                    !entity.pendingDelete &&
                    !entity.pendingSync &&
                    entity.ayahKey !in remoteByKey
                ) {
                    bookmarkDao.deleteByAyahKey(entity.ayahKey)
                }
            }

            pushPendingLocked()
        }
    }

    private suspend fun mergeRemote(key: String, dto: SavedItemDto, local: BookmarkEntity?) {
        val remoteUpdated = dto.updatedAt
        when {
            local == null ->
                dto.toEntity()?.let { bookmarkDao.upsert(it) }

            local.pendingDelete -> {
                // Local tombstone always wins (Option B): an explicit local delete is honored even
                // when the remote copy is newer. pushPendingLocked() then deletes the server item.
            }

            remoteUpdated != null && remoteUpdated > local.updatedAt ->
                dto.toEntity(localId = local.id)?.let { bookmarkDao.upsert(it) }

            else -> {
                // Local is newer-or-equal: keep local content, adopt the server id. Keep it pending
                // only if the local copy is strictly newer (an unpushed edit), so the edit publishes.
                val localNewer = remoteUpdated == null || local.updatedAt > remoteUpdated
                bookmarkDao.upsert(
                    local.copy(serverId = dto.id, pendingSync = local.pendingSync || localNewer),
                )
            }
        }
    }

    private suspend fun pullAllSavedItems(): List<SavedItemDto> {
        val all = mutableListOf<SavedItemDto>()
        var offset = 0
        while (true) {
            val page = apiCall {
                meApi.savedItems(itemType = ITEM_TYPE_QURAN_AYAH, limit = PAGE_LIMIT, offset = offset)
            }
            all += page.items
            if (page.items.size < PAGE_LIMIT) break
            offset += PAGE_LIMIT
        }
        return all
    }

    private fun SavedItemDto.toEntity(localId: Long = 0): BookmarkEntity? {
        val key = ayahKey ?: return null
        val now = Clock.System.now()
        return BookmarkEntity(
            id = localId,
            ayahKey = key,
            surahId = surahId ?: runCatching { AyahKey(key).surahId }.getOrNull() ?: return null,
            label = label,
            note = note,
            tags = tags.orEmpty(),
            createdAt = createdAt ?: now,
            updatedAt = updatedAt ?: now,
            serverId = id,
            pendingSync = false,
            pendingDelete = false,
        )
    }

    private suspend fun isAuthenticated(): Boolean =
        authSessionDataSource.authState.first() is AuthState.Authenticated

    /** Mirrors the backend note limit (<= 2000 runes); chars >= runes, so a char cap is always safe. */
    private fun sanitizeNote(note: String?): String? =
        note?.trim()?.take(MAX_NOTE_LENGTH)?.ifEmpty { null }

    /**
     * Mirrors the backend tag normalization (trim + lowercase + drop-empty + dedupe + caps) so a
     * valid local edit never round-trips to a 400 and local/remote tags converge without churn.
     */
    private fun sanitizeTags(tags: List<String>): List<String> =
        tags.asSequence()
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .map { it.take(MAX_TAG_LENGTH) }
            .distinct()
            .take(MAX_TAGS)
            .toList()

    private companion object {
        const val ITEM_TYPE_QURAN_AYAH = "quran_ayah"
        const val PAGE_LIMIT = 50

        // Mirror the backend's saved-item validation limits (internal/usecase/personal).
        const val MAX_NOTE_LENGTH = 2000
        const val MAX_TAG_LENGTH = 64
        const val MAX_TAGS = 20
    }
}
