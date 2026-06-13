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

package org.surau.app.core.data.test

import org.surau.app.core.model.data.quran.AudioSegment
import org.surau.app.core.model.data.quran.AudioTrack
import org.surau.app.core.model.data.quran.Ayah
import org.surau.app.core.model.data.quran.AyahReference
import org.surau.app.core.model.data.quran.JuzSegment
import org.surau.app.core.model.data.quran.PopulatedAyah
import org.surau.app.core.model.data.quran.Recitation
import org.surau.app.core.model.data.quran.RevelationType
import org.surau.app.core.model.data.quran.Surah
import org.surau.app.core.model.data.quran.SurahAudioManifest
import org.surau.app.core.model.data.quran.Translation
import org.surau.app.core.model.data.quran.TranslationSource

/**
 * Static Quran data for tests and previews: Al-Fatihah (complete) and the opening of
 * Al-Muzzammil, with Indonesian translations.
 */
object QuranTestData {

    const val TEST_TRANSLATION_SOURCE_ID = "kemenag-id-translation"

    val surahs = listOf(
        Surah(
            surahId = 1,
            nameArabic = "الفاتحة",
            nameLatin = "Al-Fatihah",
            nameTranslation = "Pembukaan",
            revelationType = RevelationType.MAKKIYAH,
            ayahCount = 7,
        ),
        Surah(
            surahId = 73,
            nameArabic = "المزمل",
            nameLatin = "Al-Muzzammil",
            nameTranslation = "Orang yang Berselimut",
            revelationType = RevelationType.MAKKIYAH,
            ayahCount = 20,
        ),
    )

    val translationSources = listOf(
        TranslationSource(
            id = TEST_TRANSLATION_SOURCE_ID,
            lang = "id",
            name = "Terjemahan Kemenag",
            isDefault = true,
        ),
        TranslationSource(
            id = "qul-kfgqpc-id-simple",
            lang = "id",
            name = "King Fahad Quran Complex",
        ),
    )

    const val TEST_RECITATION_ID = "mishari-al-afasy"

    val recitations = listOf(
        Recitation(
            id = TEST_RECITATION_ID,
            displayName = "Mishari Rashid Al-Afasy",
            reciterName = "Mishari Rashid Al-Afasy",
            style = "murattal",
            isDefault = true,
        ),
        Recitation(
            id = "abdul-basit-murattal",
            displayName = "Abdul Basit Abdus Samad",
            reciterName = "Abdul Basit Abdus Samad",
            style = "murattal",
        ),
    )

    /** Al-Fatihah (7 ayahs) audio manifest with fake CDN track URLs. */
    val fatihahAudioManifest = SurahAudioManifest(
        surahId = 1,
        recitationId = TEST_RECITATION_ID,
        recitationName = "Mishari Rashid Al-Afasy",
        mode = "ayah",
        tracks = (1..7).map { ayah ->
            AudioTrack(
                ayahKey = "1:$ayah",
                ayahNumber = ayah,
                url = "https://cdn.surau.test/audio/mishari/1/$ayah.mp3",
                durationMs = 3000L,
                segments = listOf(
                    AudioSegment(
                        segmentIndex = 1,
                        ayahKey = "1:$ayah",
                        timestampFromMs = 0,
                        timestampToMs = 3000,
                        durationMs = 3000L,
                    ),
                ),
            )
        },
        missingAyahKeys = emptyList(),
    )

    val juz = listOf(
        JuzSegment(
            number = 1,
            ayahCount = 148,
            start = AyahReference(1, 1, "Al-Fatihah"),
            end = AyahReference(2, 141, "Al-Baqarah"),
        ),
        JuzSegment(
            number = 29,
            ayahCount = 431,
            start = AyahReference(67, 1, "Al-Mulk"),
            end = AyahReference(77, 50, "Al-Mursalat"),
        ),
    )

    private fun fatihah(number: Int, arabic: String, indonesian: String) = PopulatedAyah(
        ayah = Ayah(
            surahId = 1,
            ayahNumber = number,
            textQpcHafs = arabic,
            pageNumber = 1,
            juzNumber = 1,
        ),
        translation = Translation(
            sourceId = TEST_TRANSLATION_SOURCE_ID,
            lang = "id",
            text = indonesian,
        ),
    )

    val ayahsBySurah: Map<Int, List<PopulatedAyah>> = mapOf(
        1 to listOf(
            fatihah(1, "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ", "Dengan nama Allah Yang Maha Pengasih, Maha Penyayang."),
            fatihah(2, "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَـٰلَمِينَ", "Segala puji bagi Allah, Tuhan seluruh alam,"),
            fatihah(3, "ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ", "Yang Maha Pengasih, Maha Penyayang,"),
            fatihah(4, "مَـٰلِكِ يَوْمِ ٱلدِّينِ", "Pemilik hari pembalasan."),
            fatihah(5, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "Hanya kepada Engkaulah kami menyembah dan hanya kepada Engkaulah kami mohon pertolongan."),
            fatihah(6, "ٱهْدِنَا ٱلصِّرَٰطَ ٱلْمُسْتَقِيمَ", "Tunjukilah kami jalan yang lurus,"),
            fatihah(7, "صِرَٰطَ ٱلَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ ٱلْمَغْضُوبِ عَلَيْهِمْ وَلَا ٱلضَّآلِّينَ", "(yaitu) jalan orang-orang yang telah Engkau beri nikmat kepadanya; bukan (jalan) mereka yang dimurkai, dan bukan (pula jalan) mereka yang sesat."),
        ),
        73 to listOf(
            PopulatedAyah(
                ayah = Ayah(73, 1, "يَـٰٓأَيُّهَا ٱلْمُزَّمِّلُ", pageNumber = 574, juzNumber = 29),
                translation = Translation(TEST_TRANSLATION_SOURCE_ID, "id", "Wahai orang yang berselimut (Muhammad)!"),
            ),
            PopulatedAyah(
                ayah = Ayah(73, 2, "قُمِ ٱلَّيْلَ إِلَّا قَلِيلًا", pageNumber = 574, juzNumber = 29),
                translation = Translation(TEST_TRANSLATION_SOURCE_ID, "id", "Bangunlah (untuk salat) pada malam hari, kecuali sebagian kecil,"),
            ),
        ),
    )
}
