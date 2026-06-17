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

/** What the player should do at a repeat boundary, decided by [RepeatLoop]. */
internal enum class RepeatAction { None, SeekToStart, Finished }

/**
 * The surah-mode repeat state machine, extracted from the player controller so the subtle
 * latch / count / boundary logic is unit-testable without a real player.
 *
 * It holds the ayah [range] to repeat, the target play count (0 = forever), the number of completed
 * passes, and an "armed" latch. The latch flips true once the position is safely before the boundary
 * and back to false on the pass that fires, so each loop counts exactly once regardless of poll
 * cadence or post-seek position lag. [onEnded] is the backstop for an end-of-file boundary that the
 * position poll misses (the player reaches STATE_ENDED first).
 */
internal class RepeatLoop {

    /** The ayah range currently looping, or null when not looping. */
    var range: IntRange? = null
        private set

    private var target = 0
    private var playsDone = 0
    private var armed = false

    val isActive: Boolean get() = range != null

    /** Starts a loop over [range] for [target] plays (0 = forever); a null [range] stops looping. */
    fun start(range: IntRange?, target: Int) {
        this.range = range
        this.target = target.coerceAtLeast(0)
        playsDone = 0
        armed = false
    }

    /** Moves a single-ayah loop to follow explicit ayah navigation (next/prev/seek). */
    fun retargetSingle(ayah: Int) {
        range = ayah..ayah
        playsDone = 0
        armed = false
    }

    fun stop() {
        range = null
        target = 0
        playsDone = 0
        armed = false
    }

    /**
     * Poll tick: [positionMs] is the current playback position and [endMs] the range's end time.
     * Arms the latch while safely before the boundary and fires exactly once when it is reached.
     */
    fun onPosition(positionMs: Long, endMs: Long): RepeatAction {
        if (range == null || endMs <= 0L) return RepeatAction.None
        if (positionMs < endMs - BOUNDARY_EPSILON_MS) {
            armed = true
            return RepeatAction.None
        }
        if (!armed) return RepeatAction.None
        armed = false
        return completePass()
    }

    /** Backstop for an end-of-file boundary the poll missed (player reported STATE_ENDED). */
    fun onEnded(): RepeatAction {
        if (range == null) return RepeatAction.None
        armed = false
        return completePass()
    }

    private fun completePass(): RepeatAction {
        if (target != 0 && ++playsDone >= target) {
            stop()
            return RepeatAction.Finished
        }
        return RepeatAction.SeekToStart
    }

    companion object {
        /**
         * The poll runs every 250ms; the boundary window must be at least that wide so a poll
         * reliably lands inside it before the track ends (otherwise [onEnded] is the backstop).
         */
        const val BOUNDARY_EPSILON_MS = 200L
    }
}
