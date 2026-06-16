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

import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ActivityHeatmapModelTest {

    private val today = LocalDate(2026, 6, 10) // a Wednesday

    @Test
    fun buildsConfiguredWeeksOfSevenDays() {
        val columns = buildHeatmapColumns(today, emptyMap())
        assertEquals(HEATMAP_WEEKS, columns.size)
        assertTrue(columns.all { it.size == HEATMAP_DAYS_PER_WEEK })
        assertEquals(HEATMAP_WEEKS * HEATMAP_DAYS_PER_WEEK, columns.flatten().size)
    }

    @Test
    fun cellsAreChronologicalAndContiguous() {
        val dates = buildHeatmapColumns(today, emptyMap()).flatten().map { it.date }
        assertEquals(dates.sorted(), dates)
        // HEATMAP_WEEKS * 7 consecutive days, no gaps/dupes.
        assertEquals(dates.size, dates.toSet().size)
    }

    @Test
    fun marksTodayAndFutureCorrectly() {
        val cells = buildHeatmapColumns(today, emptyMap()).flatten()
        assertTrue(cells.all { it.isFuture == (it.date > today) })
        val todayCell = cells.first { it.date == today }
        assertFalse(todayCell.isFuture)
        // The last column is today's week, so it contains today.
        assertTrue(buildHeatmapColumns(today, emptyMap()).last().any { it.date == today })
    }

    @Test
    fun mapsCountsByDate() {
        val earlier = LocalDate(2026, 6, 3)
        val cells = buildHeatmapColumns(today, mapOf(today to 12, earlier to 4)).flatten()
        assertEquals(12, cells.first { it.date == today }.count)
        assertEquals(4, cells.first { it.date == earlier }.count)
        assertEquals(0, cells.first { it.date == LocalDate(2026, 6, 1) }.count)
    }

    @Test
    fun bucketsIntensityLevels() {
        assertEquals(0, intensityLevel(0))
        assertEquals(1, intensityLevel(1))
        assertEquals(1, intensityLevel(2))
        assertEquals(2, intensityLevel(3))
        assertEquals(2, intensityLevel(5))
        assertEquals(3, intensityLevel(6))
        assertEquals(3, intensityLevel(10))
        assertEquals(HEATMAP_MAX_LEVEL, intensityLevel(11))
    }

    @Test
    fun futureCellHasZeroLevel() {
        assertEquals(0, HeatmapCell(today, count = 12, isFuture = true).level)
        assertEquals(HEATMAP_MAX_LEVEL, HeatmapCell(today, count = 12, isFuture = false).level)
    }
}
