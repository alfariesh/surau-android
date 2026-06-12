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
import org.surau.app.core.database.model.SurahEntity

@Dao
interface SurahDao {

    @Query("SELECT * FROM surahs ORDER BY surah_id")
    fun observeAll(): Flow<List<SurahEntity>>

    @Query("SELECT * FROM surahs WHERE surah_id = :surahId")
    fun observeSurah(surahId: Int): Flow<SurahEntity?>

    @Query("SELECT COUNT(*) FROM surahs")
    suspend fun count(): Int

    @Query("SELECT MIN(fetched_at) FROM surahs")
    suspend fun oldestFetchedAt(): Long?

    @Upsert
    suspend fun upsertAll(surahs: List<SurahEntity>)
}
