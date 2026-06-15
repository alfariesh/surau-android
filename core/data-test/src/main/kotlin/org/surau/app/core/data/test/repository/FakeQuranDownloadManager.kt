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
import org.surau.app.core.data.util.QuranDownloadManager
import org.surau.app.core.data.util.QuranDownloadState
import javax.inject.Inject

/**
 * Scriptable [QuranDownloadManager]: [downloadState] is mutable, and [startDownload] /
 * [cancelDownload] record their invocations for assertions.
 */
class FakeQuranDownloadManager @Inject constructor() : QuranDownloadManager {

    private val state = MutableStateFlow<QuranDownloadState>(QuranDownloadState.Idle)

    var startCount = 0
        private set
    var cancelCount = 0
        private set

    override val downloadState: Flow<QuranDownloadState> = state

    override fun startDownload() {
        startCount++
    }

    override fun cancelDownload() {
        cancelCount++
    }

    /** Drives [downloadState] for tests. */
    fun emit(newState: QuranDownloadState) {
        state.value = newState
    }
}
