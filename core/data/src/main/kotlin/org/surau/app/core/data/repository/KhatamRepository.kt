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

import org.surau.app.core.model.data.quran.KhatamCycle

/**
 * The Quran khatam (completion) cycle, served by the protected `/me/quran/khatam` endpoints.
 *
 * Network-first and authenticated-only (the screen shows a login CTA to guests). Suspend functions
 * throw [org.surau.app.core.network.model.SurauApiException] on backend errors and
 * [java.io.IOException] on connectivity failures, except [getActiveCycle] which maps a 404
 * (no active cycle) to `null`.
 */
interface KhatamRepository {

    /** The active (uncompleted) cycle, or `null` when the user has none. */
    suspend fun getActiveCycle(): KhatamCycle?

    /** Starts a new cycle (optional [notes]). Throws on 409 if one is already active. */
    suspend fun startCycle(notes: String?): KhatamCycle

    /** Marks one juz (1–30) as completed; idempotent. Returns the updated cycle. */
    suspend fun markJuz(juz: Int): KhatamCycle

    /** Removes one juz mark (1–30); idempotent. Returns the updated cycle. */
    suspend fun unmarkJuz(juz: Int): KhatamCycle

    /** Completes the active cycle. Throws on 409 unless all 30 juz are marked. */
    suspend fun complete(): KhatamCycle

    /** Completed cycles, newest first. */
    suspend fun history(limit: Int = 50, offset: Int = 0): List<KhatamCycle>
}
