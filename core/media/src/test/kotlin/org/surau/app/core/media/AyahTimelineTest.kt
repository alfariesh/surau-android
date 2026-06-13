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

package org.surau.app.core.media

import org.junit.Test
import org.surau.app.core.model.data.quran.AudioSegment
import org.surau.app.core.model.data.quran.AudioTrack
import org.surau.app.core.model.data.quran.SurahAudioManifest
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AyahTimelineTest {

    // Surah-mode track: segments unordered, a 2nd segment for ayah 1, ayah 3 missing, gap 8000–9000.
    private val timeline = AyahTimeline.from(
        SurahAudioManifest(
            surahId = 1,
            recitationId = "dosari",
            recitationName = "Yasser Al-Dosari",
            mode = "surah",
            tracks = listOf(
                AudioTrack(
                    ayahKey = "1",
                    ayahNumber = null,
                    url = "https://cdn/1.mp3",
                    durationMs = null,
                    segments = listOf(
                        AudioSegment(3, "1:2", 4000, 7000, 3000),
                        AudioSegment(1, "1:1", 0, 3000, 3000),
                        AudioSegment(2, "1:1", 3000, 4000, 1000),
                        AudioSegment(5, "1:5", 9000, 12000, 3000),
                        AudioSegment(4, "1:4", 7000, 8000, 1000),
                    ),
                ),
            ),
            missingAyahKeys = emptyList(),
        ),
    )

    @Test
    fun firstAndLast_areInPlaybackOrder() {
        assertEquals(1, timeline.firstAyah())
        assertEquals(5, timeline.lastAyah())
    }

    @Test
    fun ayahAt_picksLargestStartLessOrEqualToPosition() {
        assertEquals(1, timeline.ayahAt(0))
        assertEquals(1, timeline.ayahAt(3500)) // 2nd segment of ayah 1
        assertEquals(2, timeline.ayahAt(4000))
        assertEquals(4, timeline.ayahAt(7500))
        assertEquals(4, timeline.ayahAt(8500)) // gap → stays on ayah 4
        assertEquals(5, timeline.ayahAt(9000))
        assertEquals(5, timeline.ayahAt(99_999))
    }

    @Test
    fun startMsOf_usesEarliestSegment() {
        assertEquals(0, timeline.startMsOf(1))
        assertEquals(4000, timeline.startMsOf(2))
        assertEquals(9000, timeline.startMsOf(5))
        assertNull(timeline.startMsOf(3)) // missing ayah
    }

    @Test
    fun nextAndPrev_walkDistinctAyahsWithoutWrap() {
        assertEquals(4, timeline.nextAyah(2)) // ayah 3 is missing
        assertEquals(2, timeline.prevAyah(4))
        assertNull(timeline.nextAyah(5)) // last ayah, no wrap
        assertNull(timeline.prevAyah(1)) // first ayah
    }

    @Test
    fun emptyTimeline_returnsNulls() {
        val empty = AyahTimeline.from(
            SurahAudioManifest(1, "d", "D", "surah", emptyList(), emptyList()),
        )
        assertNull(empty.ayahAt(0))
        assertNull(empty.firstAyah())
    }
}
