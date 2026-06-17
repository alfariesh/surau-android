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

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Locks the surah-mode repeat state machine, including the regression that the player controller's
 * 250ms poll can miss the end-of-file boundary so [RepeatLoop.onEnded] must back it up.
 */
class RepeatLoopTest {

    private val end = 5_000L
    private val beforeBoundary = end - RepeatLoop.BOUNDARY_EPSILON_MS - 100 // safely armed
    private val inBoundary = end - RepeatLoop.BOUNDARY_EPSILON_MS + 50 // inside the fire window

    @Test
    fun inactiveByDefault() {
        val loop = RepeatLoop()
        assertFalse(loop.isActive)
        assertEquals(RepeatAction.None, loop.onPosition(beforeBoundary, end))
        assertEquals(RepeatAction.None, loop.onEnded())
    }

    @Test
    fun startWithNullRange_staysInactive() {
        val loop = RepeatLoop()
        loop.start(range = null, target = 3)
        assertFalse(loop.isActive)
        assertEquals(RepeatAction.None, loop.onEnded())
    }

    @Test
    fun foreverLoop_seeksBackEveryArmedBoundary() {
        val loop = RepeatLoop()
        loop.start(1..7, target = 0) // 0 = forever

        loop.onPosition(beforeBoundary, end) // arm
        assertEquals(RepeatAction.SeekToStart, loop.onPosition(inBoundary, end))
        assertTrue(loop.isActive)

        // Re-arm after the seek-back, then it fires again — forever.
        loop.onPosition(beforeBoundary, end)
        assertEquals(RepeatAction.SeekToStart, loop.onPosition(inBoundary, end))
    }

    @Test
    fun firesOncePerArming_noDoubleFire() {
        val loop = RepeatLoop()
        loop.start(1..1, target = 0)
        loop.onPosition(beforeBoundary, end) // arm
        assertEquals(RepeatAction.SeekToStart, loop.onPosition(inBoundary, end))
        // A second in-window poll without re-arming must not fire again.
        assertEquals(RepeatAction.None, loop.onPosition(inBoundary + 10, end))
    }

    @Test
    fun doesNotFireWithoutArming() {
        val loop = RepeatLoop()
        loop.start(1..1, target = 0)
        // First observation already in the window (e.g. right after a seek) must not fire.
        assertEquals(RepeatAction.None, loop.onPosition(inBoundary, end))
    }

    @Test
    fun finiteCount_finishesAfterRequestedPasses() {
        val loop = RepeatLoop()
        loop.start(3..3, target = 2)

        loop.onPosition(beforeBoundary, end) // arm
        assertEquals(RepeatAction.SeekToStart, loop.onPosition(inBoundary, end)) // pass 1

        loop.onPosition(beforeBoundary, end) // re-arm
        assertEquals(RepeatAction.Finished, loop.onPosition(inBoundary, end)) // pass 2 hits target
        assertFalse(loop.isActive, "loop clears itself once the count is reached")
    }

    @Test
    fun onEnded_isBackstop_whenPollMissesBoundary() {
        val loop = RepeatLoop()
        loop.start(1..7, target = 0)
        // A poll well before the end arms the latch; the next poll never lands in-window because
        // the track ends first, so STATE_ENDED -> onEnded() must seek back.
        assertEquals(RepeatAction.None, loop.onPosition(beforeBoundary, end))
        assertEquals(RepeatAction.SeekToStart, loop.onEnded())
        assertTrue(loop.isActive)
    }

    @Test
    fun onEnded_respectsFiniteCount() {
        val loop = RepeatLoop()
        loop.start(1..7, target = 1)
        assertEquals(RepeatAction.Finished, loop.onEnded())
        assertFalse(loop.isActive)
    }

    @Test
    fun stop_deactivates() {
        val loop = RepeatLoop()
        loop.start(1..1, target = 0)
        loop.onPosition(beforeBoundary, end) // arm
        loop.stop()
        assertFalse(loop.isActive)
        assertEquals(RepeatAction.None, loop.onPosition(inBoundary, end))
        assertEquals(RepeatAction.None, loop.onEnded())
    }

    @Test
    fun zeroEndMs_neverFires() {
        val loop = RepeatLoop()
        loop.start(1..1, target = 0)
        assertEquals(RepeatAction.None, loop.onPosition(0, endMs = 0))
    }

    @Test
    fun retargetSingle_movesRangeAndResetsLatch() {
        val loop = RepeatLoop()
        loop.start(1..1, target = 0)
        loop.onPosition(beforeBoundary, end) // arm on the old range
        loop.retargetSingle(5)
        assertEquals(5..5, loop.range)
        // The latch was reset by retarget, so an immediate in-window poll must not fire.
        assertEquals(RepeatAction.None, loop.onPosition(inBoundary, end))
    }
}
