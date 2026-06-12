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
 * The user's last Quran reading position. Stored locally for guests and synced to the backend
 * for signed-in users.
 *
 * @property pendingSync `true` when the local position has not been pushed to the backend yet.
 */
data class QuranReadingPosition(
    val ayahKey: AyahKey,
    val updatedAt: Instant,
    val pendingSync: Boolean = false,
) {
    val surahId: Int get() = ayahKey.surahId
    val ayahNumber: Int get() = ayahKey.ayahNumber
}
