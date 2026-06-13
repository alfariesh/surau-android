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

package org.surau.app.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import org.surau.app.core.model.data.quran.AyahKey
import org.surau.app.core.model.data.quran.QuranReadingPosition

/**
 * The single last-read Quran position (one row, id = 0). Used by guests and signed-in users
 * alike; [pendingSync] marks positions not yet pushed to the backend.
 */
@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey
    val id: Int = 0,
    @ColumnInfo(name = "ayah_key")
    val ayahKey: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
    @ColumnInfo(name = "pending_sync")
    val pendingSync: Boolean,
)

fun ReadingProgressEntity.asExternalModel() = QuranReadingPosition(
    ayahKey = AyahKey(ayahKey),
    updatedAt = updatedAt,
    pendingSync = pendingSync,
)

fun QuranReadingPosition.asEntity() = ReadingProgressEntity(
    ayahKey = ayahKey.value,
    updatedAt = updatedAt,
    pendingSync = pendingSync,
)
