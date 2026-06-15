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

package org.surau.app.core.network.model

import kotlinx.serialization.json.Json
import org.junit.Test
import org.surau.app.core.network.model.me.KhatamCycleDto
import org.surau.app.core.network.model.me.ReadingStreakDto
import org.surau.app.core.network.model.me.StartKhatamRequestDto
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Locks down the khatam/activity contract: the start-cycle body omits `notes` when absent (the
 * POST-null idiom), and the cycle/streak envelopes parse with the snake_case field names.
 */
class KhatamDtoSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun startRequest_omitsNullNotes() {
        val encoded = json.encodeToString(StartKhatamRequestDto.serializer(), StartKhatamRequestDto())
        assertFalse(encoded.contains("notes"), "notes must be omitted when null")
        assertEquals("{}", encoded)
    }

    @Test
    fun startRequest_includesProvidedNotes() {
        val encoded = json.encodeToString(
            StartKhatamRequestDto.serializer(),
            StartKhatamRequestDto(notes = "Khatam Ramadhan"),
        )
        assertTrue(encoded.contains("\"notes\":\"Khatam Ramadhan\""))
    }

    @Test
    fun khatamCycle_parsesEnvelope() {
        val cycle = json.decodeFromString(
            KhatamCycleDto.serializer(),
            """
            {
              "id": "550e8400-e29b-41d4-a716-446655440000",
              "user_id": "u1",
              "started_at": "2026-01-01T00:00:00Z",
              "completed_at": null,
              "notes": "Khatam Ramadhan",
              "completed_juz": [1, 2, 3],
              "juz_count": 3,
              "percent": 10.0,
              "created_at": "2026-01-01T00:00:00Z",
              "updated_at": "2026-01-02T00:00:00Z"
            }
            """.trimIndent(),
        )

        assertEquals(listOf(1, 2, 3), cycle.completedJuz)
        assertEquals(3, cycle.juzCount)
        assertEquals(10.0, cycle.percent)
        assertEquals(null, cycle.completedAt)
    }

    @Test
    fun streak_parsesEnvelope() {
        val streak = json.decodeFromString(
            ReadingStreakDto.serializer(),
            """
            {
              "current_streak_days": 5,
              "longest_streak_days": 12,
              "total_active_days": 40,
              "last_active_date": "2026-06-12",
              "today": "2026-06-12",
              "active_today": true
            }
            """.trimIndent(),
        )

        assertEquals(5, streak.currentStreakDays)
        assertEquals(12, streak.longestStreakDays)
        assertTrue(streak.activeToday)
    }
}
