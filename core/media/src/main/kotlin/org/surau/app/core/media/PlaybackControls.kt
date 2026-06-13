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

package org.surau.app.core.media

/** How the active surah-mode session loops while playing. */
enum class RepeatScope {
    /** No looping. */
    OFF,

    /** Loop the current ayah (follows explicit ayah navigation). */
    AYAH,

    /** Loop the whole surah. */
    SURAH,
}

/** A request to auto-stop playback; the player fades out before pausing when the timer fires. */
sealed interface SleepTimerOption {
    /** Cancel any armed timer. */
    data object Off : SleepTimerOption

    /** Stop [durationMs] from now. */
    data class After(val durationMs: Long) : SleepTimerOption

    /** Stop when the current surah finishes (disables looping so the surah can end). */
    data object EndOfSurah : SleepTimerOption
}
