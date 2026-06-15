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

package org.surau.app.core.model.data.quran

import kotlinx.datetime.Instant

/**
 * A Quran khatam (completion) cycle: the user reads all 30 juz, marking each as they finish.
 *
 * Only one cycle is active at a time. Marking the 30th juz does NOT auto-complete — completion is
 * an explicit action ([isCompletable] gates it) so an accidental mark stays reversible.
 *
 * @property completedJuz the set of juz numbers (1–30) already marked.
 * @property percent server-computed completion percentage (0–100).
 * @property completedAt non-null once the cycle has been completed (it then becomes history).
 */
data class KhatamCycle(
    val id: String,
    val startedAt: Instant?,
    val completedAt: Instant?,
    val notes: String?,
    val completedJuz: Set<Int>,
    val juzCount: Int,
    val percent: Double,
) {
    val isCompleted: Boolean get() = completedAt != null

    /** Whether all 30 juz are marked, so the cycle may be completed. */
    val isCompletable: Boolean get() = completedJuz.size >= TOTAL_JUZ

    companion object {
        const val TOTAL_JUZ = 30
    }
}
