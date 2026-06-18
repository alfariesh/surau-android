/*
 * Copyright 2024 The Android Open Source Project
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

package org.surau.app.core.datastore.test

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory [DataStore] for tests. [updateData] is serialized under a [Mutex] and runs the transform
 * exactly once per call, mirroring real DataStore's transactional read-modify-write — unlike a
 * lock-free `updateAndGet`, which can re-run the transform under contention and hide concurrency bugs.
 */
class InMemoryDataStore<T>(initialValue: T) : DataStore<T> {
    override val data = MutableStateFlow(initialValue)
    private val updateMutex = Mutex()

    override suspend fun updateData(transform: suspend (it: T) -> T): T =
        updateMutex.withLock {
            transform(data.value).also { data.value = it }
        }
}
