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
 * A saved Quran ayah ("saved item" on the backend), readable/editable offline.
 *
 * Mirrors the reading-progress offline-first contract: guests keep bookmarks locally; signed-in
 * users push them best-effort and reconcile newest-wins. [ayahKey] is the target identity (one
 * bookmark per ayah). [serverId] is the backend SavedItem UUID, `null` until the first successful
 * upload. [pendingSync] marks a local create/edit not yet pushed; [pendingDelete] is a tombstone
 * for a bookmark removed locally whose backend `DELETE` has not yet been confirmed.
 */
data class Bookmark(
    val ayahKey: AyahKey,
    val label: String? = null,
    val note: String? = null,
    val tags: List<String> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant,
    val serverId: String? = null,
    val pendingSync: Boolean = false,
    val pendingDelete: Boolean = false,
) {
    val surahId: Int get() = ayahKey.surahId
    val ayahNumber: Int get() = ayahKey.ayahNumber
}
