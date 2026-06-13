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
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.surau.app.core.database.SurauDatabase
import org.surau.app.core.database.model.AyahFetchMetadataEntity
import org.surau.app.core.network.model.PagedResponseDto
import org.surau.app.core.network.model.quran.AyahDto
import org.surau.app.core.network.model.quran.JuzDto
import org.surau.app.core.network.model.quran.QuranSearchResultDto
import org.surau.app.core.network.model.quran.RecitationDto
import org.surau.app.core.network.model.quran.SurahAudioManifestDto
import org.surau.app.core.network.model.quran.SurahDto
import org.surau.app.core.network.model.quran.TranslationSourceDto
import org.surau.app.core.network.retrofit.SurauQuranApi
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.days

@RunWith(RobolectricTestRunner::class)
class OfflineFirstQuranRepositoryTest {

    private lateinit var db: SurauDatabase
    private lateinit var quranApi: FakeSurauQuranApi
    private lateinit var subject: OfflineFirstQuranRepository

    private val sourceId = "kemenag-id-translation"

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SurauDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        quranApi = FakeSurauQuranApi()
        subject = OfflineFirstQuranRepository(
            quranApi = quranApi,
            surahDao = db.surahDao(),
            ayahDao = db.ayahDao(),
            juzDao = db.juzDao(),
            translationSourceDao = db.translationSourceDao(),
        )
    }

    @After
    fun teardown() = db.close()

    @Test
    fun ensureSurahCached_fetchesOnce_thenServesCache() = runTest {
        quranApi.ayahsResponse = PagedResponseDto(listOf(ayah(1), ayah(2)))

        subject.ensureSurahCached(surahId = 1, translationSourceId = sourceId)
        subject.ensureSurahCached(surahId = 1, translationSourceId = sourceId)

        // Second call is served from the fresh cache without hitting the network.
        assertEquals(1, quranApi.ayahsCallCount)
        assertEquals(2, subject.observeAyahs(1, sourceId).first().size)
    }

    @Test
    fun ensureSurahCached_offlineWithStaleCache_doesNotThrow() = runTest {
        // A stale cache marker forces a refresh attempt; offline, the cache is served instead.
        db.ayahDao().upsertFetchMetadata(
            AyahFetchMetadataEntity(
                surahId = 1,
                translationSourceId = sourceId,
                fetchedAt = Clock.System.now() - 60.days,
            ),
        )
        quranApi.ayahsError = IOException("offline")

        subject.ensureSurahCached(surahId = 1, translationSourceId = sourceId)

        assertEquals(1, quranApi.ayahsCallCount)
    }

    @Test
    fun ensureSurahCached_offlineWithNoCache_throwsIOException() = runTest {
        quranApi.ayahsError = IOException("offline")

        assertFailsWith<IOException> {
            subject.ensureSurahCached(surahId = 1, translationSourceId = sourceId)
        }
    }

    private fun ayah(number: Int) = AyahDto(
        surahId = 1,
        ayahNumber = number,
        ayahKey = "1:$number",
        textQpcHafs = "آية $number",
    )
}

private class FakeSurauQuranApi : SurauQuranApi {
    var ayahsResponse: PagedResponseDto<AyahDto> = PagedResponseDto(emptyList())
    var ayahsError: Exception? = null
    var ayahsCallCount = 0

    override suspend fun ayahs(
        surahId: Int,
        lang: String,
        translationSource: String?,
        includeTranslation: Boolean,
        includeAudio: Boolean,
        view: String,
    ): PagedResponseDto<AyahDto> {
        ayahsCallCount++
        ayahsError?.let { throw it }
        return ayahsResponse
    }

    override suspend fun surahs(lang: String): PagedResponseDto<SurahDto> = throw NotImplementedError()

    override suspend fun juz(): PagedResponseDto<JuzDto> = throw NotImplementedError()

    override suspend fun translationSources(lang: String?): PagedResponseDto<TranslationSourceDto> =
        throw NotImplementedError()

    override suspend fun recitations(): PagedResponseDto<RecitationDto> = throw NotImplementedError()

    override suspend fun surahAudio(
        surahId: Int,
        recitationId: String?,
    ): SurahAudioManifestDto = throw NotImplementedError()

    override suspend fun search(
        query: String,
        lang: String,
        translationSource: String?,
        limit: Int,
        offset: Int,
    ): PagedResponseDto<QuranSearchResultDto> = throw NotImplementedError()
}
