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

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import okhttp3.ResponseBody
import org.junit.Test
import org.surau.app.core.network.model.SurauApiException
import org.surau.app.core.network.model.me.CreateSavedItemRequestDto
import org.surau.app.core.network.model.me.KhatamCycleDto
import org.surau.app.core.network.model.me.KhatamHistoryResponseDto
import org.surau.app.core.network.model.me.PatchSavedItemRequestDto
import org.surau.app.core.network.model.me.PutQuranProgressRequestDto
import org.surau.app.core.network.model.me.QuranProgressDto
import org.surau.app.core.network.model.me.StartKhatamRequestDto
import org.surau.app.core.network.retrofit.SurauMeApi
import retrofit2.HttpException
import retrofit2.Response
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Exercises the real network-first khatam logic the ViewModel fakes hide: 404 → null, error
 * propagation, DTO → model mapping (juz set dedupe), and blank-notes → null on start.
 */
class DefaultKhatamRepositoryTest {

    private val api = FakeKhatamApi()
    private val subject = DefaultKhatamRepository(api)

    @Test
    fun getActiveCycle_returnsNull_on404() = runTest {
        api.activeResult = { throw http(404) }

        assertNull(subject.getActiveCycle())
    }

    @Test
    fun getActiveCycle_rethrows_onNon404() = runTest {
        api.activeResult = { throw http(500) }

        assertFailsWith<SurauApiException> { subject.getActiveCycle() }
    }

    @Test
    fun getActiveCycle_mapsDtoToModel_dedupingJuzToSet() = runTest {
        api.activeResult = {
            cycle("c-1", completedJuz = listOf(1, 2, 2, 3), juzCount = 3, percent = 10.0, notes = "Ramadhan")
        }

        val cycle = subject.getActiveCycle()!!
        assertEquals("c-1", cycle.id)
        assertEquals(setOf(1, 2, 3), cycle.completedJuz)
        assertEquals(3, cycle.juzCount)
        assertEquals(10.0, cycle.percent, 0.0)
        assertEquals("Ramadhan", cycle.notes)
    }

    @Test
    fun startCycle_blankNotes_sentAsNull_butRealNotesKept() = runTest {
        subject.startCycle("   ")
        subject.startCycle("Ramadhan 1447")

        assertEquals(listOf(null, "Ramadhan 1447"), api.startNotes)
    }

    @Test
    fun markAndUnmarkJuz_callTheRightEndpoint() = runTest {
        subject.markJuz(5)
        subject.unmarkJuz(7)

        assertEquals(listOf(5), api.markedJuz)
        assertEquals(listOf(7), api.unmarkedJuz)
    }

    @Test
    fun history_mapsList_newestFirst() = runTest {
        api.historyResult = listOf(
            cycle("c-1", completedAt = Instant.fromEpochSeconds(100)),
            cycle("c-2"),
        )

        val history = subject.history()
        assertEquals(listOf("c-1", "c-2"), history.map { it.id })
        assertTrue(history.first().isCompleted)
    }
}

private class FakeKhatamApi : SurauMeApi {
    var activeResult: () -> KhatamCycleDto = { throw http(404) }
    var startResult: KhatamCycleDto = cycle("c-new")
    var juzResult: KhatamCycleDto = cycle("c-1")
    var historyResult: List<KhatamCycleDto> = emptyList()
    val startNotes = mutableListOf<String?>()
    val markedJuz = mutableListOf<Int>()
    val unmarkedJuz = mutableListOf<Int>()

    override suspend fun activeKhatam(): KhatamCycleDto = activeResult()

    override suspend fun startKhatam(body: StartKhatamRequestDto): KhatamCycleDto {
        startNotes += body.notes
        return startResult
    }

    override suspend fun markKhatamJuz(juz: Int): KhatamCycleDto {
        markedJuz += juz
        return juzResult
    }

    override suspend fun unmarkKhatamJuz(juz: Int): KhatamCycleDto {
        unmarkedJuz += juz
        return juzResult
    }

    override suspend fun completeKhatam(): KhatamCycleDto = juzResult

    override suspend fun khatamHistory(limit: Int, offset: Int) =
        KhatamHistoryResponseDto(items = historyResult, total = historyResult.size)

    // Endpoints irrelevant to this repository.
    override suspend fun quranProgress(): QuranProgressDto = throw notUsed()
    override suspend fun putQuranProgress(body: PutQuranProgressRequestDto): QuranProgressDto = throw notUsed()
    override suspend fun savedItems(itemType: String, surahId: Int?, tag: String?, limit: Int, offset: Int) = throw notUsed()
    override suspend fun savedItemTags() = throw notUsed()
    override suspend fun createSavedItem(body: CreateSavedItemRequestDto) = throw notUsed()
    override suspend fun patchSavedItem(id: String, body: PatchSavedItemRequestDto) = throw notUsed()
    override suspend fun deleteSavedItem(id: String) = throw notUsed()
    override suspend fun activity(from: String, to: String) = throw notUsed()
    override suspend fun streak(today: String) = throw notUsed()
    override suspend fun surahProgress() = throw notUsed()
}

private fun cycle(
    id: String,
    completedJuz: List<Int> = emptyList(),
    juzCount: Int = completedJuz.size,
    percent: Double = 0.0,
    notes: String? = null,
    completedAt: Instant? = null,
) = KhatamCycleDto(
    id = id,
    startedAt = Instant.fromEpochSeconds(1),
    completedAt = completedAt,
    notes = notes,
    completedJuz = completedJuz,
    juzCount = juzCount,
    percent = percent,
)

private fun http(status: Int) = HttpException(
    Response.error<Any>(status, ResponseBody.create(null, """{"code":"X"}""")),
)

private fun notUsed() = UnsupportedOperationException("endpoint not used in DefaultKhatamRepositoryTest")
