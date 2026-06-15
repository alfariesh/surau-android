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

package org.surau.app.core.network.model.me

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A Quran khatam (completion) cycle: one active cycle per user, completed by marking all 30 juz.
 * Numeric/date fields carry defensive defaults so a partial payload still deserializes.
 */
@Serializable
data class KhatamCycleDto(
    @SerialName("id") val id: String,
    @SerialName("started_at") val startedAt: Instant? = null,
    @SerialName("completed_at") val completedAt: Instant? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("completed_juz") val completedJuz: List<Int> = emptyList(),
    @SerialName("juz_count") val juzCount: Int = 0,
    @SerialName("percent") val percent: Double = 0.0,
    @SerialName("created_at") val createdAt: Instant? = null,
    @SerialName("updated_at") val updatedAt: Instant? = null,
)

/**
 * Body for `POST /me/quran/khatam` (start a cycle). [notes] is optional; under the app's `Json`
 * (encodeDefaults off) a `null` value equals its default and is omitted from the request entirely.
 */
@Serializable
data class StartKhatamRequestDto(
    @SerialName("notes") val notes: String? = null,
)

@Serializable
data class KhatamHistoryResponseDto(
    @SerialName("items") val items: List<KhatamCycleDto> = emptyList(),
    @SerialName("total") val total: Int = 0,
)

/**
 * Daily reading-activity buckets plus an aggregate over `[from, to]`. Dates are `YYYY-MM-DD`
 * strings (parsed to `LocalDate` in the data layer).
 */
@Serializable
data class ReadingActivitySummaryDto(
    @SerialName("from") val from: String? = null,
    @SerialName("to") val to: String? = null,
    @SerialName("active_days") val activeDays: Int = 0,
    @SerialName("quran_ayahs_read") val quranAyahsRead: Int = 0,
    @SerialName("kitab_pages_read") val kitabPagesRead: Int = 0,
    @SerialName("quran_active_days") val quranActiveDays: Int = 0,
    @SerialName("kitab_active_days") val kitabActiveDays: Int = 0,
    @SerialName("days") val days: List<ReadingActivityDayDto> = emptyList(),
)

@Serializable
data class ReadingActivityDayDto(
    @SerialName("date") val date: String = "",
    @SerialName("quran_ayahs_read") val quranAyahsRead: Int = 0,
    @SerialName("kitab_pages_read") val kitabPagesRead: Int = 0,
    @SerialName("quran_events") val quranEvents: Int = 0,
    @SerialName("kitab_events") val kitabEvents: Int = 0,
)

/**
 * The consecutive-day reading streak. `today`/`last_active_date` are `YYYY-MM-DD` strings.
 */
@Serializable
data class ReadingStreakDto(
    @SerialName("current_streak_days") val currentStreakDays: Int = 0,
    @SerialName("longest_streak_days") val longestStreakDays: Int = 0,
    @SerialName("total_active_days") val totalActiveDays: Int = 0,
    @SerialName("last_active_date") val lastActiveDate: String? = null,
    @SerialName("today") val today: String? = null,
    @SerialName("active_today") val activeToday: Boolean = false,
)

/**
 * Per-surah resume positions (each [QuranProgressDto] carries `surah_id` + `position_percent`),
 * used for the thin progress badges on the surah list.
 */
@Serializable
data class QuranProgressListResponseDto(
    @SerialName("items") val items: List<QuranProgressDto> = emptyList(),
    @SerialName("total") val total: Int = 0,
)
