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

package org.surau.app.core.data.test.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock
import org.surau.app.core.data.repository.QuranProgressRepository
import org.surau.app.core.model.data.quran.AyahKey
import org.surau.app.core.model.data.quran.QuranReadingPosition
import javax.inject.Inject

/**
 * Fake [QuranProgressRepository] holding the position in memory.
 */
class FakeQuranProgressRepository @Inject constructor() : QuranProgressRepository {

    private val position = MutableStateFlow<QuranReadingPosition?>(null)

    override fun observePosition(): Flow<QuranReadingPosition?> = position

    override suspend fun savePosition(ayahKey: AyahKey) {
        position.value = QuranReadingPosition(
            ayahKey = ayahKey,
            updatedAt = Clock.System.now(),
            pendingSync = false,
        )
    }

    override suspend fun pushPendingPosition() = Unit

    override suspend fun reconcile() = Unit

    var clearLocalDataCount = 0
        private set

    override suspend fun clearLocalData() {
        clearLocalDataCount++
        position.value = null
    }
}
