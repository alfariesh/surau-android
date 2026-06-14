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

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.surau.app.core.database.SurauDatabase
import org.surau.app.core.datastore.AuthSession
import org.surau.app.core.datastore.AuthSessionDataSource
import org.surau.app.core.datastore.test.InMemoryDataStore
import org.surau.app.core.model.data.auth.UserSession
import org.surau.app.core.model.data.quran.AyahKey
import org.surau.app.core.network.model.me.CreateSavedItemRequestDto
import org.surau.app.core.network.model.me.PatchSavedItemRequestDto
import org.surau.app.core.network.model.me.PutQuranProgressRequestDto
import org.surau.app.core.network.model.me.QuranProgressDto
import org.surau.app.core.network.model.me.SavedItemDto
import org.surau.app.core.network.model.me.SavedItemsResponseDto
import org.surau.app.core.network.model.me.SavedItemsTagsResponseDto
import org.surau.app.core.network.retrofit.SurauMeApi
import retrofit2.HttpException
import retrofit2.Response
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeBookmarkApi : SurauMeApi {
    /** The server's saved-items, keyed by id. */
    val store = mutableMapOf<String, SavedItemDto>()
    val created = mutableListOf<CreateSavedItemRequestDto>()
    val patched = mutableListOf<Pair<String, PatchSavedItemRequestDto>>()
    val deleted = mutableListOf<String>()
    var listThrows404 = false
    private var nextId = 1

    fun seed(dto: SavedItemDto) {
        store[dto.id] = dto
    }

    override suspend fun savedItems(
        itemType: String,
        surahId: Int?,
        tag: String?,
        limit: Int,
        offset: Int,
    ): SavedItemsResponseDto {
        if (listThrows404) throw http404()
        val all = store.values.toList()
        val page = all.drop(offset).take(limit)
        return SavedItemsResponseDto(items = page, total = all.size)
    }

    override suspend fun savedItemTags() =
        SavedItemsTagsResponseDto(items = store.values.flatMap { it.tags.orEmpty() }.distinct())

    override suspend fun createSavedItem(body: CreateSavedItemRequestDto): SavedItemDto {
        created += body
        // Upsert by target: reuse the existing id for the same ayah, else mint a new one.
        val existing = store.values.firstOrNull { it.ayahKey == body.ayahKey }
        val id = existing?.id ?: "server-${nextId++}"
        val dto = SavedItemDto(
            id = id,
            itemType = body.itemType,
            surahId = body.surahId,
            ayahKey = body.ayahKey,
            label = body.label ?: existing?.label,
            note = body.note ?: existing?.note,
            tags = body.tags ?: existing?.tags,
            createdAt = existing?.createdAt ?: Instant.fromEpochSeconds(1),
            updatedAt = Clock.System.now(),
        )
        store[id] = dto
        return dto
    }

    override suspend fun patchSavedItem(id: String, body: PatchSavedItemRequestDto): SavedItemDto {
        patched += id to body
        val current = store[id] ?: throw http404()
        val dto = current.copy(note = body.note, tags = body.tags, updatedAt = Clock.System.now())
        store[id] = dto
        return dto
    }

    override suspend fun deleteSavedItem(id: String) {
        deleted += id
        if (store.remove(id) == null) throw http404()
    }

    override suspend fun quranProgress(): QuranProgressDto = throw http404()
    override suspend fun putQuranProgress(body: PutQuranProgressRequestDto): QuranProgressDto =
        throw UnsupportedOperationException()

    private fun http404() = HttpException(
        Response.error<Any>(404, ResponseBody.create(null, """{"code":"NOT_FOUND"}""")),
    )
}

@RunWith(RobolectricTestRunner::class)
class OfflineFirstBookmarkRepositoryTest {

    private lateinit var db: SurauDatabase
    private lateinit var meApi: FakeBookmarkApi
    private lateinit var authSession: AuthSessionDataSource
    private lateinit var subject: OfflineFirstBookmarkRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SurauDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        meApi = FakeBookmarkApi()
        authSession = AuthSessionDataSource(InMemoryDataStore(AuthSession.getDefaultInstance()))
        subject = OfflineFirstBookmarkRepository(
            bookmarkDao = db.bookmarkDao(),
            meApi = meApi,
            authSessionDataSource = authSession,
        )
    }

    @After
    fun teardown() = db.close()

    private suspend fun signIn() {
        authSession.setSession(
            session = UserSession("user-1", "user@surau.org", null, "session-1"),
            accessToken = "access",
            refreshToken = "refresh",
            expiresAtEpochSeconds = Clock.System.now().epochSeconds + 900,
        )
    }

    private fun seededDto(
        id: String,
        ayahKey: String,
        note: String? = null,
        tags: List<String> = emptyList(),
        updatedAt: Instant,
    ) = SavedItemDto(
        id = id,
        itemType = "quran_ayah",
        surahId = AyahKey(ayahKey).surahId,
        ayahKey = ayahKey,
        note = note,
        tags = tags,
        createdAt = Instant.fromEpochSeconds(1),
        updatedAt = updatedAt,
    )

    @Test
    fun addBookmark_asGuest_isLocalAndPending_noNetwork() = runTest {
        subject.addBookmark(AyahKey("73:4"), note = "renungan", tags = listOf("tafsir"))

        val bookmark = subject.observeBookmark(AyahKey("73:4")).first()
        assertEquals("73:4", bookmark?.ayahKey?.value)
        assertEquals("renungan", bookmark?.note)
        assertEquals(listOf("tafsir"), bookmark?.tags)
        assertTrue(bookmark!!.pendingSync)
        assertNull(bookmark.serverId)
        assertTrue(meApi.created.isEmpty())
    }

    @Test
    fun addBookmark_whenSignedIn_postsAndStoresServerId() = runTest {
        signIn()

        subject.addBookmark(AyahKey("73:4"), note = "renungan")

        assertEquals(listOf("73:4"), meApi.created.map(CreateSavedItemRequestDto::ayahKey))
        val bookmark = subject.observeBookmark(AyahKey("73:4")).first()
        assertEquals("server-1", bookmark?.serverId)
        assertFalse(bookmark!!.pendingSync)
    }

    @Test
    fun updateBookmark_patchesWithExplicitNullNote() = runTest {
        signIn()
        subject.addBookmark(AyahKey("73:4"), note = "old", tags = listOf("a"))

        subject.updateBookmark(AyahKey("73:4"), note = null, tags = listOf("b", "c"))

        val (id, body) = meApi.patched.last()
        assertEquals("server-1", id)
        assertNull(body.note) // cleared
        assertEquals(listOf("b", "c"), body.tags)
        val bookmark = subject.observeBookmark(AyahKey("73:4")).first()
        assertNull(bookmark?.note)
        assertEquals(listOf("b", "c"), bookmark?.tags)
        assertFalse(bookmark!!.pendingSync)
    }

    @Test
    fun removeBookmark_whenSynced_tombstonesThenDeletes() = runTest {
        signIn()
        subject.addBookmark(AyahKey("73:4"))

        subject.removeBookmark(AyahKey("73:4"))

        assertEquals(listOf("server-1"), meApi.deleted)
        assertTrue(subject.observeBookmarks().first().isEmpty())
    }

    @Test
    fun removeBookmark_whenGuestUnsynced_hardDeletes_noNetwork() = runTest {
        subject.addBookmark(AyahKey("73:4"))

        subject.removeBookmark(AyahKey("73:4"))

        assertTrue(meApi.deleted.isEmpty())
        assertTrue(subject.observeBookmarks().first().isEmpty())
    }

    @Test
    fun reconcile_remoteOnly_inserted() = runTest {
        signIn()
        meApi.seed(seededDto("srv-1", "2:255", note = "ayat kursi", updatedAt = future()))

        subject.reconcile()

        val bookmark = subject.observeBookmark(AyahKey("2:255")).first()
        assertEquals("srv-1", bookmark?.serverId)
        assertEquals("ayat kursi", bookmark?.note)
        assertFalse(bookmark!!.pendingSync)
    }

    @Test
    fun reconcile_remoteNewer_overwritesLocal() = runTest {
        signIn()
        // Local synced copy (older), then a newer server copy of the same ayah.
        meApi.seed(seededDto("srv-1", "73:4", note = "old", updatedAt = Instant.fromEpochSeconds(10)))
        subject.reconcile()
        meApi.store["srv-1"] = seededDto("srv-1", "73:4", note = "new", updatedAt = future())

        subject.reconcile()

        assertEquals("new", subject.observeBookmark(AyahKey("73:4")).first()?.note)
    }

    @Test
    fun reconcile_guestLocal_pushedAndDedupedByTarget() = runTest {
        // Guest bookmarks 73:4; the same ayah already exists server-side (another device).
        subject.addBookmark(AyahKey("73:4"), note = "mine")
        meApi.seed(seededDto("srv-existing", "73:4", note = "theirs", updatedAt = Instant.fromEpochSeconds(5)))
        signIn()

        subject.reconcile()

        val all = subject.observeBookmarks().first()
        assertEquals(1, all.size) // no duplicate target
        assertEquals("srv-existing", all.single().serverId)
        // Local is newer, so it PATCHes its content over the server copy (no second create).
        assertTrue(meApi.created.isEmpty())
        assertEquals("srv-existing", meApi.patched.last().first)
    }

    @Test
    fun reconcile_removedServerSide_deletesLocal() = runTest {
        signIn()
        subject.addBookmark(AyahKey("73:4")) // creates server-1, synced
        meApi.store.clear() // deleted on another device

        subject.reconcile()

        assertTrue(subject.observeBookmarks().first().isEmpty())
    }

    @Test
    fun reconcile_listReturns404_treatedAsEmpty_pushesGuestLocal() = runTest {
        subject.addBookmark(AyahKey("73:4")) // guest, pending
        signIn()
        meApi.listThrows404 = true

        subject.reconcile()

        // 404 is treated as "no remote", so the guest bookmark still gets created.
        assertEquals(listOf("73:4"), meApi.created.map(CreateSavedItemRequestDto::ayahKey))
        assertFalse(subject.observeBookmark(AyahKey("73:4")).first()!!.pendingSync)
    }

    @Test
    fun observeTags_derivesDistinctSortedTags() = runTest {
        subject.addBookmark(AyahKey("1:1"), tags = listOf("b", "a"))
        subject.addBookmark(AyahKey("2:255"), tags = listOf("a", "c"))

        assertEquals(listOf("a", "b", "c"), subject.observeTags().first())
    }

    private fun future(): Instant = Clock.System.now() + kotlin.time.Duration.parse("1h")
}
