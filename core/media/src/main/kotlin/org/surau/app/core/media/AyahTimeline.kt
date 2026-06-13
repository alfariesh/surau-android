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

import org.surau.app.core.model.data.quran.SurahAudioManifest

/**
 * Maps playback position to ayah for a surah-mode track (one audio file per surah). Built from the
 * manifest's segments: each ayah's earliest start, sorted ascending. Segments may arrive unordered
 * and multiple per ayah; both collapse here.
 */
internal class AyahTimeline private constructor(
    private val entries: List<Entry>,
) {
    private data class Entry(val startMs: Long, val ayah: Int)

    /**
     * The ayah playing at [positionMs] = the ayah whose start is the largest `<= positionMs`. Before
     * the first start it stays on the first ayah; across a gap it stays on the previous ayah.
     */
    fun ayahAt(positionMs: Long): Int? {
        if (entries.isEmpty()) return null
        var lo = 0
        var hi = entries.lastIndex
        var ans = 0
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (entries[mid].startMs <= positionMs) {
                ans = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return entries[ans].ayah
    }

    /** The earliest start (ms) of [ayah], or `null` if it has no segment. */
    fun startMsOf(ayah: Int): Long? = entries.firstOrNull { it.ayah == ayah }?.startMs

    /**
     * The end (ms) of [ayah] = the next ayah's start, or `null` at the last ayah (the caller falls
     * back to the track duration). Used to detect the loop boundary while repeating.
     */
    fun endMsOf(ayah: Int): Long? = nextAyah(ayah)?.let(::startMsOf)

    fun firstAyah(): Int? = entries.firstOrNull()?.ayah

    fun lastAyah(): Int? = entries.lastOrNull()?.ayah

    /** The ayah after [current] in playback order, or `null` at the last ayah (no wrap). */
    fun nextAyah(current: Int): Int? {
        val i = entries.indexOfFirst { it.ayah == current }
        return if (i in 0 until entries.lastIndex) entries[i + 1].ayah else null
    }

    /** The ayah before [current] in playback order, or `null` at the first ayah. */
    fun prevAyah(current: Int): Int? {
        val i = entries.indexOfFirst { it.ayah == current }
        return if (i > 0) entries[i - 1].ayah else null
    }

    companion object {
        fun from(manifest: SurahAudioManifest): AyahTimeline {
            val entries = manifest.tracks
                .asSequence()
                .flatMap { it.segments.asSequence() }
                .mapNotNull { segment ->
                    val ayah = segment.ayahKey.substringAfter(':', "").toIntOrNull()
                        ?: return@mapNotNull null
                    ayah to segment.timestampFromMs
                }
                .groupBy({ it.first }, { it.second })
                .map { (ayah, starts) -> Entry(startMs = starts.min(), ayah = ayah) }
                .sortedBy { it.startMs }
            return AyahTimeline(entries)
        }
    }
}
