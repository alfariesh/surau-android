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

package org.surau.app.feature.activity.impl

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Heatmap layout constants. Weeks are Sunday-first to match the local calendar convention. */
internal const val HEATMAP_WEEKS = 5
internal const val HEATMAP_DAYS_PER_WEEK = 7

/** Highest intensity level (0 = none … [HEATMAP_MAX_LEVEL] = most). */
internal const val HEATMAP_MAX_LEVEL = 4

/**
 * One day cell of the activity heatmap. Pure data — no Compose — so the grid layout and intensity
 * bucketing can be unit-tested without a UI.
 */
internal data class HeatmapCell(
    val date: LocalDate,
    val count: Int,
    val isFuture: Boolean,
) {
    /** Intensity bucket 0..[HEATMAP_MAX_LEVEL]; future days are 0 (rendered transparent). */
    val level: Int = if (isFuture) 0 else intensityLevel(count)
}

/** Buckets a day's Quran-ayah count into an intensity level 0..[HEATMAP_MAX_LEVEL]. */
internal fun intensityLevel(count: Int): Int = when {
    count <= 0 -> 0
    count <= 2 -> 1
    count <= 5 -> 2
    count <= 10 -> 3
    else -> HEATMAP_MAX_LEVEL
}

/**
 * Builds [weeks] columns × 7 rows (Sunday…Saturday) ending at the column that contains [today].
 * Cells after [today] are marked [HeatmapCell.isFuture]. Counts come from [countsByDate].
 */
internal fun buildHeatmapColumns(
    today: LocalDate,
    countsByDate: Map<LocalDate, Int>,
    weeks: Int = HEATMAP_WEEKS,
): List<List<HeatmapCell>> {
    val sundayOffset = today.dayOfWeek.isoDayNumber % 7 // Sun -> 0 … Sat -> 6
    val lastColumnStart = today.minus(sundayOffset, DateTimeUnit.DAY)
    val gridStart = lastColumnStart.minus((weeks - 1) * HEATMAP_DAYS_PER_WEEK, DateTimeUnit.DAY)
    return (0 until weeks).map { col ->
        (0 until HEATMAP_DAYS_PER_WEEK).map { row ->
            val date = gridStart.plus(col * HEATMAP_DAYS_PER_WEEK + row, DateTimeUnit.DAY)
            HeatmapCell(date = date, count = countsByDate[date] ?: 0, isFuture = date > today)
        }
    }
}
