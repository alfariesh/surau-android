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

package org.surau.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.surau.app.core.database.model.JuzEntity

@Dao
interface JuzDao {

    @Query("SELECT * FROM juz ORDER BY number")
    fun observeAll(): Flow<List<JuzEntity>>

    @Query("SELECT COUNT(*) FROM juz")
    suspend fun count(): Int

    @Upsert
    suspend fun upsertAll(juz: List<JuzEntity>)
}
