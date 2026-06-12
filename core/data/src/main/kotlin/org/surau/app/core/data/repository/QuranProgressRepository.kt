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
import org.surau.app.core.model.data.quran.AyahKey
import org.surau.app.core.model.data.quran.QuranReadingPosition

/**
 * The user's last-read Quran position. Local-first: every save lands in Room immediately
 * (guest included); for signed-in users the position is pushed to the backend and reconciled
 * on login by newest-wins.
 */
interface QuranProgressRepository {

    /** The locally stored position, or `null` when the user has never read. */
    fun observePosition(): Flow<QuranReadingPosition?>

    /**
     * Saves [ayahKey] locally now. Marks it pending sync; call [pushPendingPosition] (debounced)
     * to publish it for signed-in users.
     */
    suspend fun savePosition(ayahKey: AyahKey)

    /**
     * Pushes the pending local position to the backend if the user is signed in. Safe to call
     * when there is nothing to push. Never throws — failures keep the position pending.
     */
    suspend fun pushPendingPosition()

    /**
     * Reconciles local and remote positions (newest observation wins). Called after login and
     * by the background sync worker.
     */
    suspend fun reconcile()
}
