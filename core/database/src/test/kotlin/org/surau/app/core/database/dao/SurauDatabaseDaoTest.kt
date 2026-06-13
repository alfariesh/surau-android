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

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.surau.app.core.database.SurauDatabase
import org.surau.app.core.database.model.AyahEntity
import org.surau.app.core.database.model.AyahFetchMetadataEntity
import org.surau.app.core.database.model.ReadingProgressEntity
import org.surau.app.core.database.model.SurahEntity
import org.surau.app.core.database.model.TranslationEntity
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class SurauDatabaseDaoTest {

    private lateinit var db: SurauDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SurauDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() = db.close()

    @Test
    fun surahDao_upsertsAndObservesInOrder() = runTest {
        db.surahDao().upsertAll(
            listOf(
                surahEntity(2, "البقرة", "Al-Baqarah"),
                surahEntity(1, "الفاتحة", "Al-Fatihah"),
            ),
        )

        val surahs = db.surahDao().observeAll().first()
        assertEquals(listOf(1, 2), surahs.map(SurahEntity::surahId))
        assertEquals(2, db.surahDao().count())
    }

    @Test
    fun ayahDao_joinsTranslationForRequestedSource() = runTest {
        db.ayahDao().upsertSurahContent(
            ayahs = listOf(
                AyahEntity(1, 1, "1:1", "بِسْمِ ٱللَّهِ", 1, 1, 1),
                AyahEntity(1, 2, "1:2", "ٱلْحَمْدُ لِلَّهِ", 1, 1, 1),
            ),
            translations = listOf(
                TranslationEntity(1, 1, "kemenag-id", "id", "Dengan nama Allah"),
                TranslationEntity(1, 1, "other-en", "en", "In the name of Allah"),
            ),
            transliterations = emptyList(),
            metadata = AyahFetchMetadataEntity(1, "kemenag-id", Instant.fromEpochSeconds(100)),
        )

        val rows = db.ayahDao().observePopulatedAyahs(1, "kemenag-id").first()

        assertEquals(2, rows.size)
        assertEquals("Dengan nama Allah", rows[0].translationText)
        // Ayah without a translation row still appears, with null translation.
        assertNull(rows[1].translationText)

        assertNotNull(db.ayahDao().fetchMetadata(1, "kemenag-id"))
        assertNull(db.ayahDao().fetchMetadata(1, "other-en"))
    }

    @Test
    fun readingProgressDao_upsertsSingleRowAndMarksSynced() = runTest {
        db.readingProgressDao().upsert(
            ReadingProgressEntity(
                ayahKey = "73:4",
                updatedAt = Instant.fromEpochSeconds(100),
                pendingSync = true,
            ),
        )
        db.readingProgressDao().upsert(
            ReadingProgressEntity(
                ayahKey = "2:255",
                updatedAt = Instant.fromEpochSeconds(200),
                pendingSync = true,
            ),
        )

        val progress = db.readingProgressDao().observeProgress().first()
        assertNotNull(progress)
        assertEquals("2:255", progress.ayahKey)
        assertTrue(progress.pendingSync)

        db.readingProgressDao().markSynced("2:255")
        assertFalse(db.readingProgressDao().progress()!!.pendingSync)
    }

    private fun surahEntity(id: Int, arabic: String, latin: String) = SurahEntity(
        surahId = id,
        nameArabic = arabic,
        nameLatin = latin,
        nameTranslation = latin,
        revelationType = "makkiyah",
        ayahCount = 7,
        info = null,
        fetchedAt = Instant.fromEpochSeconds(50),
    )
}
