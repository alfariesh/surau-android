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

package org.surau.app.core.sync.test

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.surau.app.core.data.util.QuranDownloadManager
import org.surau.app.core.data.util.QuranDownloadState
import javax.inject.Inject

internal class IdleQuranDownloadManager @Inject constructor() : QuranDownloadManager {
    override val downloadState: Flow<QuranDownloadState> = flowOf(QuranDownloadState.Idle)
    override fun startDownload() = Unit
    override fun cancelDownload() = Unit
}
