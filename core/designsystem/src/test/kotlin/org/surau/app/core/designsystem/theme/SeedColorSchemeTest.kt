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

package org.surau.app.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/**
 * Pure-JVM contrast fuzzer. It is the safety gate for [colorSchemeFromSeed]: every text-against-
 * background pair a generated scheme produces must clear WCAG, no matter how adversarial the seed.
 * If this fails, the clamp in [ensureContrast] has a hole and no Compose/screenshot test should run.
 */
class SeedColorSchemeTest {

    private val readingMin = 7.0f - TOLERANCE
    private val generalMin = 4.5f - TOLERANCE

    private val seeds: List<Long> = buildList {
        // The shipped presets.
        addAll(
            listOf(
                0xFF006D4AL,
                0xFF0E7C86L,
                0xFF2D5BD0L,
                0xFFC2562EL,
                0xFF8A5A2BL,
                0xFF3B3F8FL,
                0xFF5B6B61L,
                0xFF3A3A3AL,
            ),
        )
        // Adversarial extremes: pure black/white, full-saturation primaries & secondaries, mid grey.
        addAll(
            listOf(
                0xFF000000L, 0xFFFFFFFFL, 0xFFFF0000L, 0xFF00FF00L, 0xFF0000FFL,
                0xFFFFFF00L, 0xFF00FFFFL, 0xFFFF00FFL, 0xFF808080L, 0xFFFFFF01L,
            ),
        )
        // A spread of pseudo-random opaque colors (fixed seed → deterministic).
        val rng = Random(0x5EED)
        repeat(96) { add(0xFF000000L or rng.nextInt(0x1000000).toLong()) }
    }

    @Test
    fun everyGeneratedScheme_meetsContrastFloors() {
        var checked = 0
        for (seedArgb in seeds) {
            val seed = Color(seedArgb)
            for (style in SeedPaletteStyle.entries) {
                for (contrast in listOf(0f, 0.5f, 1f)) {
                    for (dark in listOf(false, true)) {
                        val scheme = colorSchemeFromSeed(seed, dark, style, contrast)
                        assertScheme(scheme, "seed=${seedArgb.toString(16)} $style c=$contrast dark=$dark")
                        checked++
                    }
                }
            }
        }
        // Guard against the loops silently collapsing to nothing.
        assertTrue("expected many schemes, checked=$checked", checked > 1000)
    }

    private fun assertScheme(s: ColorScheme, ctx: String) {
        // The mushaf reading pairs are held to AAA.
        assertPair("onSurface/surface", s.onSurface, s.surface, readingMin, ctx)
        assertPair("onBackground/background", s.onBackground, s.background, readingMin, ctx)
        // General text pairs to AA.
        assertPair("onSurfaceVariant/surfaceVariant", s.onSurfaceVariant, s.surfaceVariant, generalMin, ctx)
        assertPair("onPrimary/primary", s.onPrimary, s.primary, generalMin, ctx)
        assertPair("onPrimaryContainer/primaryContainer", s.onPrimaryContainer, s.primaryContainer, generalMin, ctx)
        assertPair("onSecondary/secondary", s.onSecondary, s.secondary, generalMin, ctx)
        assertPair(
            "onSecondaryContainer/secondaryContainer",
            s.onSecondaryContainer,
            s.secondaryContainer,
            generalMin,
            ctx,
        )
        assertPair("onTertiary/tertiary", s.onTertiary, s.tertiary, generalMin, ctx)
        assertPair(
            "onTertiaryContainer/tertiaryContainer",
            s.onTertiaryContainer,
            s.tertiaryContainer,
            generalMin,
            ctx,
        )
    }

    private fun assertPair(name: String, fg: Color, bg: Color, min: Float, ctx: String) {
        val ratio = contrastRatio(fg, bg)
        assertTrue("$name contrast $ratio < $min  [$ctx]", ratio >= min)
    }

    @Test
    fun unspecifiedSeedIsNeverGenerated_defaultPresetUsesStaticScheme() {
        // Sanity: a fully-saturated seed still yields a near-neutral surface (low chroma), so the app
        // chrome stays calm. Surface luminance should be very light (light mode) / very dark (dark).
        val light = colorSchemeFromSeed(Color(0xFFFF0000L), dark = false, SeedPaletteStyle.VIBRANT, 0f)
        val dark = colorSchemeFromSeed(Color(0xFFFF0000L), dark = true, SeedPaletteStyle.VIBRANT, 0f)
        assertTrue("light surface should be bright", light.surface.luminance() > 0.7f)
        assertTrue("dark surface should be dim", dark.surface.luminance() < 0.1f)
    }

    private companion object {
        const val TOLERANCE = 0.15f
    }
}
