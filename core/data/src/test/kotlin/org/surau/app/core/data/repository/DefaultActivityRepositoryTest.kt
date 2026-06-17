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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import okhttp3.ResponseBody
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.surau.app.core.data.util.NetworkMonitor
import org.surau.app.core.datastore.AuthSession
import org.surau.app.core.datastore.AuthSessionDataSource
import org.surau.app.core.datastore.test.InMemoryDataStore
import org.surau.app.core.model.data.auth.UserSession
import org.surau.app.core.network.model.me.CreateSavedItemRequestDto
import org.surau.app.core.network.model.me.PatchSavedItemRequestDto
import org.surau.app.core.network.model.me.PutQuranProgressRequestDto
import org.surau.app.core.network.model.me.QuranProgressDto
import org.surau.app.core.network.model.me.QuranProgressListResponseDto
import org.surau.app.core.network.model.me.ReadingActivityDayDto
import org.surau.app.core.network.model.me.ReadingActivitySummaryDto
import org.surau.app.core.network.model.me.ReadingStreakDto
import org.surau.app.core.network.model.me.StartKhatamRequestDto
import org.surau.app.core.network.retrofit.SurauMeApi
import retrofit2.HttpException
import retrofit2.Response
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Exercises the real network-first activity logic: the exact `from`/`to`/`today` ISO params sent to
 * the backend, DTO → model mapping (dropping unparseable dates), and the guest/failure → empty-map
 * degradation of per-surah progress — including the placeholder-first emission of
 * [DefaultActivityRepository.observeSurahProgress] that the [getSurahProgress] one-shot avoids.
 */
@RunWith(RobolectricTestRunner::class)
class DefaultActivityRepositoryTest {

    private lateinit var api: FakeActivityApi
    private lateinit var authSession: AuthSessionDataSource
    private lateinit var subject: DefaultActivityRepository

    @Before
    fun setup() {
        api = FakeActivityApi()
        authSession = AuthSessionDataSource(InMemoryDataStore(AuthSession.getDefaultInstance()))
        subject = DefaultActivityRepository(
            meApi = api,
            authSessionDataSource = authSession,
            networkMonitor = FakeNetworkMonitor(online = true),
        )
    }

    private suspend fun signIn() {
        authSession.setSession(
            session = UserSession("user-1", "user@surau.org", null, "session-1"),
            accessToken = "access",
            refreshToken = "refresh",
            expiresAtEpochSeconds = Clock.System.now().epochSeconds + 900,
        )
    }

    @Test
    fun getActivity_sendsIsoDates_andDropsUnparseableDays() = runTest {
        api.activityResult = ReadingActivitySummaryDto(
            from = "2026-04-01",
            to = "2026-06-10",
            activeDays = 2,
            quranAyahsRead = 50,
            days = listOf(
                ReadingActivityDayDto(date = "2026-06-09", quranAyahsRead = 10),
                ReadingActivityDayDto(date = "garbage", quranAyahsRead = 5),
                ReadingActivityDayDto(date = "", quranAyahsRead = 3),
            ),
        )

        val result = subject.getActivity(LocalDate(2026, 4, 1), LocalDate(2026, 6, 10))

        assertEquals("2026-04-01", api.capturedFrom)
        assertEquals("2026-06-10", api.capturedTo)
        assertEquals(50, result.totalQuranAyahs)
        // The garbage + blank dates are dropped; only the parseable day survives.
        assertEquals(listOf(LocalDate(2026, 6, 9)), result.days.map { it.date })
    }

    @Test
    fun getStreak_sendsIsoToday_andMapsFields() = runTest {
        api.streakResult = ReadingStreakDto(
            currentStreakDays = 3,
            longestStreakDays = 7,
            totalActiveDays = 20,
            lastActiveDate = "2026-06-09",
            activeToday = true,
        )

        val streak = subject.getStreak(LocalDate(2026, 6, 10))

        assertEquals("2026-06-10", api.capturedToday)
        assertEquals(3, streak.currentStreakDays)
        assertEquals(7, streak.longestStreakDays)
        assertEquals(LocalDate(2026, 6, 9), streak.lastActiveDate)
        assertTrue(streak.activeToday)
    }

    @Test
    fun getSurahProgress_guest_returnsEmpty_withoutHittingNetwork() = runTest {
        api.surahProgressResult = { error("guest must not call the network") }

        assertTrue(subject.getSurahProgress().isEmpty())
    }

    @Test
    fun getSurahProgress_authed_mapsPercentToFraction() = runTest {
        signIn()
        api.surahProgressResult = {
            QuranProgressListResponseDto(
                items = listOf(QuranProgressDto(surahId = 2, ayahNumber = 1, ayahKey = "2:1", positionPercent = 50.0)),
            )
        }

        assertEquals(0.5f, subject.getSurahProgress()[2])
    }

    @Test
    fun getSurahProgress_authed_failureDegradesToEmpty() = runTest {
        signIn()
        api.surahProgressResult = { throw http(500) }

        assertTrue(subject.getSurahProgress().isEmpty())
    }

    @Test
    fun observeSurahProgress_guest_emitsOnlyPlaceholder() = runTest {
        val emissions = subject.observeSurahProgress().toList()

        assertEquals(listOf(emptyMap<Int, Float>()), emissions)
    }

    @Test
    fun observeSurahProgress_offline_emitsOnlyPlaceholder_withoutFetching() = runTest {
        signIn()
        api.surahProgressResult = { error("must not fetch while offline") }
        val offline = DefaultActivityRepository(
            meApi = api,
            authSessionDataSource = authSession,
            networkMonitor = FakeNetworkMonitor(online = false),
        )

        assertEquals(listOf(emptyMap<Int, Float>()), offline.observeSurahProgress().toList())
    }

    @Test
    fun observeSurahProgress_authed_emitsPlaceholderThenRealMap() = runTest {
        signIn()
        api.surahProgressResult = {
            QuranProgressListResponseDto(
                items = listOf(QuranProgressDto(surahId = 2, ayahNumber = 1, ayahKey = "2:1", positionPercent = 50.0)),
            )
        }

        val emissions = subject.observeSurahProgress().toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions.first().isEmpty())
        assertEquals(0.5f, emissions.last()[2])
    }
}

private class FakeActivityApi : SurauMeApi {
    var capturedFrom: String? = null
    var capturedTo: String? = null
    var capturedToday: String? = null
    var activityResult = ReadingActivitySummaryDto()
    var streakResult = ReadingStreakDto()
    var surahProgressResult: () -> QuranProgressListResponseDto = { QuranProgressListResponseDto() }

    override suspend fun activity(from: String, to: String): ReadingActivitySummaryDto {
        capturedFrom = from
        capturedTo = to
        return activityResult
    }

    override suspend fun streak(today: String): ReadingStreakDto {
        capturedToday = today
        return streakResult
    }

    override suspend fun surahProgress(): QuranProgressListResponseDto = surahProgressResult()

    // Endpoints irrelevant to this repository.
    override suspend fun quranProgress(): QuranProgressDto = throw notUsed()
    override suspend fun putQuranProgress(body: PutQuranProgressRequestDto): QuranProgressDto = throw notUsed()
    override suspend fun savedItems(itemType: String, surahId: Int?, tag: String?, limit: Int, offset: Int) = throw notUsed()
    override suspend fun savedItemTags() = throw notUsed()
    override suspend fun createSavedItem(body: CreateSavedItemRequestDto) = throw notUsed()
    override suspend fun patchSavedItem(id: String, body: PatchSavedItemRequestDto) = throw notUsed()
    override suspend fun deleteSavedItem(id: String) = throw notUsed()
    override suspend fun activeKhatam() = throw notUsed()
    override suspend fun startKhatam(body: StartKhatamRequestDto) = throw notUsed()
    override suspend fun markKhatamJuz(juz: Int) = throw notUsed()
    override suspend fun unmarkKhatamJuz(juz: Int) = throw notUsed()
    override suspend fun completeKhatam() = throw notUsed()
    override suspend fun khatamHistory(limit: Int, offset: Int) = throw notUsed()
}

private class FakeNetworkMonitor(online: Boolean) : NetworkMonitor {
    override val isOnline: Flow<Boolean> = flowOf(online)
}

private fun http(status: Int) = HttpException(
    Response.error<Any>(status, ResponseBody.create(null, """{"code":"X"}""")),
)

private fun notUsed() = UnsupportedOperationException("endpoint not used in DefaultActivityRepositoryTest")
