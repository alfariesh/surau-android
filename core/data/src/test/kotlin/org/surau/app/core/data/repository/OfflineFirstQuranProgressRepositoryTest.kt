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
import org.surau.app.core.network.model.me.PutQuranProgressRequestDto
import org.surau.app.core.network.model.me.QuranProgressDto
import org.surau.app.core.network.retrofit.SurauMeApi
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeMeApi : SurauMeApi {
    var remote: QuranProgressDto? = null
    val pushed = mutableListOf<PutQuranProgressRequestDto>()

    override suspend fun quranProgress(): QuranProgressDto =
        remote ?: throw retrofit2.HttpException(
            retrofit2.Response.error<Any>(
                404,
                okhttp3.ResponseBody.create(null, """{"error":"not found","code":"NOT_FOUND"}"""),
            ),
        )

    override suspend fun putQuranProgress(body: PutQuranProgressRequestDto): QuranProgressDto {
        pushed += body
        val key = AyahKey(body.ayahKey)
        return QuranProgressDto(
            surahId = key.surahId,
            ayahNumber = key.ayahNumber,
            ayahKey = body.ayahKey,
            observedAt = body.clientObservedAt,
        )
    }

    override suspend fun savedItems(
        itemType: String,
        surahId: Int?,
        tag: String?,
        limit: Int,
        offset: Int,
    ) = org.surau.app.core.network.model.me.SavedItemsResponseDto()

    override suspend fun savedItemTags() =
        org.surau.app.core.network.model.me.SavedItemsTagsResponseDto()

    override suspend fun createSavedItem(
        body: org.surau.app.core.network.model.me.CreateSavedItemRequestDto,
    ): org.surau.app.core.network.model.me.SavedItemDto = throw UnsupportedOperationException()

    override suspend fun patchSavedItem(
        id: String,
        body: org.surau.app.core.network.model.me.PatchSavedItemRequestDto,
    ): org.surau.app.core.network.model.me.SavedItemDto = throw UnsupportedOperationException()

    override suspend fun deleteSavedItem(id: String) = throw UnsupportedOperationException()
}

@RunWith(RobolectricTestRunner::class)
class OfflineFirstQuranProgressRepositoryTest {

    private lateinit var db: SurauDatabase
    private lateinit var meApi: FakeMeApi
    private lateinit var authSession: AuthSessionDataSource
    private lateinit var subject: OfflineFirstQuranProgressRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SurauDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        meApi = FakeMeApi()
        authSession = AuthSessionDataSource(InMemoryDataStore(AuthSession.getDefaultInstance()))
        subject = OfflineFirstQuranProgressRepository(
            readingProgressDao = db.readingProgressDao(),
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

    @Test
    fun savePosition_isLocalAndPending() = runTest {
        subject.savePosition(AyahKey("73:4"))

        val position = subject.observePosition().first()
        assertEquals("73:4", position?.ayahKey?.value)
        assertTrue(position!!.pendingSync)
    }

    @Test
    fun pushPendingPosition_asGuest_doesNothing() = runTest {
        subject.savePosition(AyahKey("73:4"))

        subject.pushPendingPosition()

        assertTrue(meApi.pushed.isEmpty())
        assertTrue(subject.observePosition().first()!!.pendingSync)
    }

    @Test
    fun pushPendingPosition_whenSignedIn_pushesAndMarksSynced() = runTest {
        signIn()
        subject.savePosition(AyahKey("73:4"))

        subject.pushPendingPosition()

        assertEquals(listOf("73:4"), meApi.pushed.map(PutQuranProgressRequestDto::ayahKey))
        assertFalse(subject.observePosition().first()!!.pendingSync)
    }

    @Test
    fun reconcile_remoteNewer_adoptsRemote() = runTest {
        signIn()
        subject.savePosition(AyahKey("1:1"))
        meApi.remote = QuranProgressDto(
            surahId = 2,
            ayahNumber = 255,
            ayahKey = "2:255",
            observedAt = Clock.System.now() + kotlin.time.Duration.parse("1h"),
        )

        subject.reconcile()

        val position = subject.observePosition().first()
        assertEquals("2:255", position?.ayahKey?.value)
        assertFalse(position!!.pendingSync)
        assertTrue(meApi.pushed.isEmpty())
    }

    @Test
    fun reconcile_localNewer_pushesLocal() = runTest {
        signIn()
        meApi.remote = QuranProgressDto(
            surahId = 2,
            ayahNumber = 255,
            ayahKey = "2:255",
            observedAt = Instant.fromEpochSeconds(1_000),
        )
        subject.savePosition(AyahKey("73:4"))

        subject.reconcile()

        assertEquals(listOf("73:4"), meApi.pushed.map(PutQuranProgressRequestDto::ayahKey))
        assertEquals("73:4", subject.observePosition().first()?.ayahKey?.value)
    }

    @Test
    fun reconcile_noRemoteNoLocal_isNoOp() = runTest {
        signIn()

        subject.reconcile()

        assertNull(subject.observePosition().first())
        assertTrue(meApi.pushed.isEmpty())
    }
}
