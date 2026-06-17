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
import kotlinx.datetime.LocalDate
import org.surau.app.core.model.data.activity.ReadingActivity
import org.surau.app.core.model.data.activity.ReadingStreak

/**
 * Reading activity & streak, served by the protected `/me/activity*` and `/me/quran/progress`
 * endpoints. Network-first and authenticated-only.
 *
 * The underlying data is produced as a side effect of saving reading progress (already wired in
 * [QuranProgressRepository]); this repository only reads it back.
 */
interface ActivityRepository {

    /** Daily activity buckets over `[from, to]` (dates in the device's local calendar). */
    suspend fun getActivity(from: LocalDate, to: LocalDate): ReadingActivity

    /** The reading streak; [today] is the device's local date so day boundaries match the device. */
    suspend fun getStreak(today: LocalDate): ReadingStreak

    /**
     * Per-surah completion fraction (surahId → 0f..1f) for the thin progress badges on the surah
     * list. Authenticated-only: emits an empty map for guests, then the fetched map; failures
     * degrade to empty rather than surfacing an error (the badges are non-essential).
     */
    fun observeSurahProgress(): Flow<Map<Int, Float>>

    /**
     * One-shot per-surah completion fraction (surahId → 0f..1f). Authenticated-only: returns an
     * empty map for guests or when the fetch fails. Unlike [observeSurahProgress] this resolves to
     * the fetched value directly, so callers that need a snapshot (not a live stream) must use this
     * — collecting [observeSurahProgress] with `first()` would only ever see the placeholder.
     */
    suspend fun getSurahProgress(): Map<Int, Float>
}
