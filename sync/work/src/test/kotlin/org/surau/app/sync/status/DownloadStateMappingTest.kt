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

package org.surau.app.sync.status

import androidx.work.WorkInfo
import org.junit.Test
import org.surau.app.core.data.util.QuranDownloadState
import kotlin.test.assertEquals

class DownloadStateMappingTest {

    @Test
    fun noWork_orCancelled_isIdle() {
        assertEquals(QuranDownloadState.Idle, resolveDownloadState(null, null, SOURCE_A, 0))
        assertEquals(
            QuranDownloadState.Idle,
            resolveDownloadState(WorkInfo.State.CANCELLED, SOURCE_A, SOURCE_A, 50),
        )
    }

    @Test
    fun succeeded_forActiveSource_isCompleted() {
        assertEquals(
            QuranDownloadState.Completed,
            resolveDownloadState(WorkInfo.State.SUCCEEDED, SOURCE_A, SOURCE_A, 100),
        )
    }

    @Test
    fun succeeded_forDifferentSource_isIdle() {
        // The headline fix: a download finished for source A but the user now reads source B, which
        // has no offline index — so report Idle (prompt a fresh download), not a false "Completed".
        assertEquals(
            QuranDownloadState.Idle,
            resolveDownloadState(WorkInfo.State.SUCCEEDED, SOURCE_A, SOURCE_B, 100),
        )
    }

    @Test
    fun succeeded_legacyWorkWithoutSource_isCompleted() {
        // A pre-existing completed download that never published a source key is assumed to match.
        assertEquals(
            QuranDownloadState.Completed,
            resolveDownloadState(WorkInfo.State.SUCCEEDED, null, SOURCE_A, 100),
        )
    }

    @Test
    fun failed_isFailed() {
        assertEquals(
            QuranDownloadState.Failed,
            resolveDownloadState(WorkInfo.State.FAILED, SOURCE_A, SOURCE_A, 30),
        )
    }

    @Test
    fun running_forActiveSource_reportsProgress() {
        assertEquals(
            QuranDownloadState.Running(42),
            resolveDownloadState(WorkInfo.State.RUNNING, SOURCE_A, SOURCE_A, 42),
        )
        // Not yet published (enqueued) → assume the active source, show 0%.
        assertEquals(
            QuranDownloadState.Running(0),
            resolveDownloadState(WorkInfo.State.ENQUEUED, null, SOURCE_A, 0),
        )
    }

    @Test
    fun running_forDifferentSource_isIdle() {
        assertEquals(
            QuranDownloadState.Idle,
            resolveDownloadState(WorkInfo.State.RUNNING, SOURCE_A, SOURCE_B, 42),
        )
    }

    private companion object {
        const val SOURCE_A = "en.sahih"
        const val SOURCE_B = "id.indonesian"
    }
}
