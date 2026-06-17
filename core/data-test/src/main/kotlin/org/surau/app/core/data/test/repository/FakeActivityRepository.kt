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

package org.surau.app.core.data.test.repository

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate
import org.surau.app.core.data.repository.ActivityRepository
import org.surau.app.core.model.data.activity.ReadingActivity
import org.surau.app.core.model.data.activity.ReadingStreak
import java.io.IOException
import javax.inject.Inject

/**
 * Fake [ActivityRepository] returning canned activity/streak/per-surah-progress from memory.
 * Set [failLoads] to make [getActivity]/[getStreak] throw (to exercise the screen's error state).
 */
class FakeActivityRepository @Inject constructor() : ActivityRepository {

    private val surahProgress = MutableStateFlow<Map<Int, Float>>(emptyMap())

    var activity: ReadingActivity = ReadingActivity(
        from = null,
        to = null,
        activeDays = 0,
        totalQuranAyahs = 0,
        days = emptyList(),
    )

    var streak: ReadingStreak = ReadingStreak(
        currentStreakDays = 0,
        longestStreakDays = 0,
        totalActiveDays = 0,
        lastActiveDate = null,
        activeToday = false,
    )

    var failLoads: Boolean = false

    /** When set, [getStreak] suspends on this until completed — lets a test hold a load in flight. */
    var loadGate: CompletableDeferred<Unit>? = null

    fun setSurahProgress(map: Map<Int, Float>) {
        surahProgress.value = map
    }

    override suspend fun getActivity(from: LocalDate, to: LocalDate): ReadingActivity {
        if (failLoads) throw IOException("fake activity failure")
        return activity
    }

    override suspend fun getStreak(today: LocalDate): ReadingStreak {
        loadGate?.await()
        if (failLoads) throw IOException("fake streak failure")
        return streak
    }

    override fun observeSurahProgress(): Flow<Map<Int, Float>> = surahProgress

    override suspend fun getSurahProgress(): Map<Int, Float> = surahProgress.value
}
