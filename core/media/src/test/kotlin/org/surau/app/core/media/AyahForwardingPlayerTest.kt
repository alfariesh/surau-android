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
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AyahForwardingPlayerTest {

    // Ayah starts (ms) for a 5-ayah surah-mode track.
    private val starts = longArrayOf(0, 5_000, 12_000, 18_000, 25_000)

    @Test
    fun nextAyahStart_isFollowingCut_orNullAtLast() {
        assertEquals(5_000, nextAyahStartMs(starts, positionMs = 0))
        assertEquals(5_000, nextAyahStartMs(starts, positionMs = 3_000)) // still in ayah 1
        assertEquals(12_000, nextAyahStartMs(starts, positionMs = 5_000))
        assertEquals(25_000, nextAyahStartMs(starts, positionMs = 20_000)) // in ayah 4 → ayah 5
        assertNull(nextAyahStartMs(starts, positionMs = 26_000)) // last ayah, no wrap
    }

    @Test
    fun previousAyahStart_restartsWhenPastThreshold_elseStepsBack() {
        // Just into ayah 3 (< 1s) → step back to ayah 2.
        assertEquals(5_000, previousAyahStartMs(starts, positionMs = 12_500, restartThresholdMs = 1_000))
        // Well into ayah 3 (> 1s) → restart ayah 3.
        assertEquals(12_000, previousAyahStartMs(starts, positionMs = 15_000, restartThresholdMs = 1_000))
        // At the very first ayah → clamp to its start.
        assertEquals(0, previousAyahStartMs(starts, positionMs = 500, restartThresholdMs = 1_000))
    }

    @Test
    fun emptyStarts_areHandled() {
        assertNull(nextAyahStartMs(longArrayOf(), positionMs = 1_000))
        assertEquals(0, previousAyahStartMs(longArrayOf(), positionMs = 1_000, restartThresholdMs = 1_000))
    }
}
