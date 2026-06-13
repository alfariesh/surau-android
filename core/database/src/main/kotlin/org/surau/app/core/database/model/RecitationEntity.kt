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
import org.surau.app.core.model.data.quran.Recitation

/**
 * An available Quran recitation (reciter + style), cached from `GET /quran/recitations`.
 */
@Entity(tableName = "recitations")
data class RecitationEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "reciter_name")
    val reciterName: String,
    val style: String?,
    @ColumnInfo(name = "mode")
    val mode: String?,
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean,
    @ColumnInfo(name = "fetched_at")
    val fetchedAt: Instant,
)

fun RecitationEntity.asExternalModel() = Recitation(
    id = id,
    displayName = displayName,
    reciterName = reciterName,
    style = style,
    mode = mode,
    isDefault = isDefault,
)
