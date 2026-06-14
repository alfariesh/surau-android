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
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.surau.app.core.database.SurauDatabase
import org.surau.app.core.network.model.PagedResponseDto
import org.surau.app.core.network.model.quran.AudioManifestRecitationDto
import org.surau.app.core.network.model.quran.AyahDto
import org.surau.app.core.network.model.quran.JuzDto
import org.surau.app.core.network.model.quran.QuranSearchResultDto
import org.surau.app.core.network.model.quran.RecitationDto
import org.surau.app.core.network.model.quran.SurahAudioManifestDto
import org.surau.app.core.network.model.quran.SurahAudioTrackDto
import org.surau.app.core.network.model.quran.SurahDto
import org.surau.app.core.network.model.quran.TranslationSourceDto
import org.surau.app.core.network.retrofit.SurauQuranApi
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class OfflineFirstQuranAudioRepositoryTest {

    private lateinit var db: SurauDatabase
    private lateinit var quranApi: FakeAudioQuranApi
    private lateinit var subject: OfflineFirstQuranAudioRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SurauDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        quranApi = FakeAudioQuranApi()
        subject = OfflineFirstQuranAudioRepository(
            quranApi = quranApi,
            recitationDao = db.recitationDao(),
        )
    }

    @After
    fun teardown() = db.close()

    @Test
    fun resolveRecitationId_withPreference_returnsItWithoutNetwork() = runTest {
        quranApi.recitationsResponse = recitations(default = "afasy", other = "basit")

        assertEquals("basit", subject.resolveRecitationId("basit"))
        assertEquals(0, quranApi.recitationsCallCount)
    }

    @Test
    fun resolveRecitationId_noPreference_fallsBackToDefault() = runTest {
        quranApi.recitationsResponse = recitations(default = "afasy", other = "basit")

        assertEquals("afasy", subject.resolveRecitationId(null))
    }

    @Test
    fun resolveRecitationId_emptyOffline_returnsNull() = runTest {
        quranApi.recitationsError = IOException("offline")

        assertNull(subject.resolveRecitationId(null))
    }

    @Test
    fun resolveRecitationId_byMode_picksMatchingMode() = runTest {
        quranApi.recitationsResponse = PagedResponseDto(
            listOf(
                RecitationDto(id = "afasy", displayName = "Afasy", reciterName = "Afasy", mode = "ayah", isDefault = true),
                RecitationDto(id = "dosari", displayName = "Dosari", reciterName = "Dosari", mode = "surah"),
            ),
        )

        assertEquals("dosari", subject.resolveRecitationId(null, "surah"))
        // An ayah-mode preference is ignored when surah mode is required.
        assertEquals("dosari", subject.resolveRecitationId("afasy", "surah"))
    }

    @Test
    fun observeRecitations_fetchesOnce_thenServesCache() = runTest {
        quranApi.recitationsResponse = recitations(default = "afasy", other = "basit")

        assertEquals(2, subject.observeRecitations().first().size)
        subject.observeRecitations().first()

        assertEquals(1, quranApi.recitationsCallCount)
    }

    @Test
    fun audioManifest_mapsDto_withDisplayNameFallback() = runTest {
        quranApi.audioResponse = manifestDto()

        val manifest = subject.audioManifest(surahId = 1, recitationId = "afasy")

        assertEquals(1, manifest.surahId)
        // display_name is blank, so the reciter name is used instead.
        assertEquals("Afasy", manifest.recitationName)
        assertEquals(listOf(1, 2), manifest.tracks.map { it.ayahNumber })
        assertEquals(listOf("1:3"), manifest.missingAyahKeys)
    }

    @Test
    fun audioManifest_offline_throwsIOException() = runTest {
        quranApi.audioError = IOException("offline")

        assertFailsWith<IOException> { subject.audioManifest(surahId = 1, recitationId = "afasy") }
    }

    private fun recitations(default: String, other: String) = PagedResponseDto(
        listOf(
            RecitationDto(id = other, displayName = other, reciterName = other, isDefault = false),
            RecitationDto(id = default, displayName = default, reciterName = default, isDefault = true),
        ),
    )

    private fun manifestDto() = SurahAudioManifestDto(
        surahId = 1,
        recitation = AudioManifestRecitationDto(
            id = "afasy",
            displayName = "",
            reciterName = "Afasy",
            isDefault = true,
        ),
        mode = "ayah",
        tracks = listOf(
            SurahAudioTrackDto(trackKey = "1:1", surahId = 1, ayahNumber = 1, url = "https://x/1.mp3", durationMs = 3000),
            SurahAudioTrackDto(trackKey = "1:2", surahId = 1, ayahNumber = 2, url = "https://x/2.mp3", durationMs = 3000),
        ),
        missingAyahKeys = listOf("1:3"),
    )
}

private class FakeAudioQuranApi : SurauQuranApi {
    var recitationsResponse: PagedResponseDto<RecitationDto> = PagedResponseDto(emptyList())
    var recitationsError: Exception? = null
    var recitationsCallCount = 0

    var audioResponse: SurahAudioManifestDto? = null
    var audioError: Exception? = null

    override suspend fun recitations(): PagedResponseDto<RecitationDto> {
        recitationsCallCount++
        recitationsError?.let { throw it }
        return recitationsResponse
    }

    override suspend fun surahAudio(surahId: Int, recitationId: String?): SurahAudioManifestDto {
        audioError?.let { throw it }
        return audioResponse ?: throw NotImplementedError()
    }

    override suspend fun surahs(lang: String): PagedResponseDto<SurahDto> = throw NotImplementedError()

    override suspend fun ayahs(
        surahId: Int,
        lang: String,
        translationSource: String?,
        includeTranslation: Boolean,
        includeAudio: Boolean,
        view: String,
    ): PagedResponseDto<AyahDto> = throw NotImplementedError()

    override suspend fun juz(): PagedResponseDto<JuzDto> = throw NotImplementedError()

    override suspend fun translationSources(lang: String?): PagedResponseDto<TranslationSourceDto> =
        throw NotImplementedError()

    override suspend fun search(
        query: String,
        lang: String,
        translationSource: String?,
        limit: Int,
        offset: Int,
    ): PagedResponseDto<QuranSearchResultDto> = throw NotImplementedError()
}
