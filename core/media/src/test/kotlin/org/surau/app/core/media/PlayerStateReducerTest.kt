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

import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class PlayerStateReducerTest {

    @Test
    fun derivesSurahAndAyahFromCurrentItem() {
        val state = reducePlayerState(
            base = PlayerUiState(),
            isPlaying = true,
            currentItem = mediaItem(surahId = 1, ayah = 4),
            durationMs = 3000L,
        )

        assertTrue(state.isPlaying)
        assertEquals(1, state.surahId)
        assertEquals(4, state.currentAyahNumber)
        assertEquals("Mishari", state.recitationName)
        assertEquals(3000L, state.durationMs)
    }

    @Test
    fun nullItem_clearsPlaybackIdentity() {
        val state = reducePlayerState(
            base = PlayerUiState(surahId = 1, currentAyahNumber = 2),
            isPlaying = false,
            currentItem = null,
            durationMs = 0L,
        )

        assertFalse(state.isPlaying)
        assertNull(state.surahId)
        assertNull(state.currentAyahNumber)
    }

    @Test
    fun unsetDuration_isCoercedToZero() {
        val state = reducePlayerState(
            base = PlayerUiState(),
            isPlaying = false,
            currentItem = null,
            durationMs = C.TIME_UNSET,
        )

        assertEquals(0L, state.durationMs)
    }

    private fun mediaItem(surahId: Int, ayah: Int): MediaItem {
        val extras = Bundle().apply {
            putInt(KEY_SURAH_ID, surahId)
            putInt(KEY_AYAH_NUMBER, ayah)
        }
        return MediaItem.Builder()
            .setMediaId("$surahId:$ayah")
            .setMediaMetadata(MediaMetadata.Builder().setArtist("Mishari").setExtras(extras).build())
            .build()
    }
}
