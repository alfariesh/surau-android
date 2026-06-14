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

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player

/**
 * Wraps the player so a surah-mode track — one audio file with per-ayah cut points stashed in its
 * MediaItem extras ([KEY_AYAH_STARTS]) — skips ayah-by-ayah from the notification, lock screen, and
 * headset buttons. Those controls drive `seekToNext`/`seekToPrevious`, which by default only move
 * between MediaItems; in surah mode there is just one, so they would otherwise be dead.
 *
 * When the current item has no ayah starts (ayah mode, or no session), every command falls through
 * to the wrapped player unchanged.
 */
internal class AyahForwardingPlayer(player: Player) : ForwardingPlayer(player) {

    private fun ayahStarts(): LongArray? =
        currentMediaItem?.mediaMetadata?.extras
            ?.getLongArray(KEY_AYAH_STARTS)
            ?.takeIf { it.isNotEmpty() }

    override fun getAvailableCommands(): Player.Commands {
        val base = super.getAvailableCommands()
        return if (ayahStarts() == null) {
            base
        } else {
            base.buildUpon()
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .build()
        }
    }

    override fun isCommandAvailable(command: Int): Boolean =
        if (ayahStarts() != null &&
            (command == Player.COMMAND_SEEK_TO_NEXT || command == Player.COMMAND_SEEK_TO_PREVIOUS)
        ) {
            true
        } else {
            super.isCommandAvailable(command)
        }

    override fun seekToNext() {
        if (!skipToNextAyah()) super.seekToNext()
    }

    override fun seekToNextMediaItem() {
        if (!skipToNextAyah()) super.seekToNextMediaItem()
    }

    override fun seekToPrevious() {
        if (!skipToPreviousAyah()) super.seekToPrevious()
    }

    override fun seekToPreviousMediaItem() {
        if (!skipToPreviousAyah()) super.seekToPreviousMediaItem()
    }

    /** Seeks to the next ayah (no-op at the last one). Returns false in ayah mode (let super run). */
    private fun skipToNextAyah(): Boolean {
        val starts = ayahStarts() ?: return false
        nextAyahStartMs(starts, currentPosition)?.let(::seekTo)
        return true
    }

    private fun skipToPreviousAyah(): Boolean {
        val starts = ayahStarts() ?: return false
        seekTo(previousAyahStartMs(starts, currentPosition, AYAH_RESTART_THRESHOLD_MS))
        return true
    }

    private companion object {
        const val AYAH_RESTART_THRESHOLD_MS = 1_000L
    }
}

/** Index of the ayah playing at [positionMs] = the largest index whose start is `<= positionMs`. */
private fun currentAyahIndex(startsAsc: LongArray, positionMs: Long): Int {
    var lo = 0
    var hi = startsAsc.lastIndex
    var ans = 0
    while (lo <= hi) {
        val mid = (lo + hi) ushr 1
        if (startsAsc[mid] <= positionMs) {
            ans = mid
            lo = mid + 1
        } else {
            hi = mid - 1
        }
    }
    return ans
}

/** Start (ms) of the ayah after the one playing at [positionMs], or `null` at the last ayah. */
internal fun nextAyahStartMs(startsAsc: LongArray, positionMs: Long): Long? {
    if (startsAsc.isEmpty()) return null
    val index = currentAyahIndex(startsAsc, positionMs)
    return if (index < startsAsc.lastIndex) startsAsc[index + 1] else null
}

/**
 * Target of a "previous" press: restart the current ayah if more than [restartThresholdMs] into it,
 * otherwise the previous ayah's start (clamped at the first ayah).
 */
internal fun previousAyahStartMs(startsAsc: LongArray, positionMs: Long, restartThresholdMs: Long): Long {
    if (startsAsc.isEmpty()) return 0L
    val index = currentAyahIndex(startsAsc, positionMs)
    val current = startsAsc[index]
    return when {
        positionMs - current > restartThresholdMs -> current
        index > 0 -> startsAsc[index - 1]
        else -> current
    }
}
