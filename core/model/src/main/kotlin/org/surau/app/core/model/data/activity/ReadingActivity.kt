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

package org.surau.app.core.model.data.activity

import kotlinx.datetime.LocalDate

/**
 * Daily reading-activity buckets over `[from, to]`, used to render the activity heatmap.
 *
 * @property days one bucket per day that had activity (days with none are simply absent).
 */
data class ReadingActivity(
    val from: LocalDate?,
    val to: LocalDate?,
    val activeDays: Int,
    val totalQuranAyahs: Int,
    val days: List<ReadingActivityDay>,
) {
    /** Quick lookup of a day's Quran ayah count by date for placing heatmap cells. */
    val quranAyahsByDate: Map<LocalDate, Int> = days.associate { it.date to it.quranAyahsRead }
}

data class ReadingActivityDay(
    val date: LocalDate,
    val quranAyahsRead: Int,
    val kitabPagesRead: Int,
)
