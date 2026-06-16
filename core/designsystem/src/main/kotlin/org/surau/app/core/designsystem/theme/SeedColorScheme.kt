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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/**
 * How vivid a [colorSchemeFromSeed] result is. The chroma of the seed is scaled by [chromaScale]
 * before building the primary/secondary/tertiary tonal palettes.
 */
enum class SeedPaletteStyle(val chromaScale: Float) {
    /** Near-monochrome — the seed barely tints the UI. */
    NEUTRAL(0.30f),

    /** Balanced, Material You-like saturation. The default. */
    TONAL_SPOT(1.0f),

    /** Punchier accents. */
    VIBRANT(1.45f),

    /** Boldest accents. */
    EXPRESSIVE(1.9f),
}

/**
 * Builds a full Material 3 [ColorScheme] from a single seed [Color], the way Material You does — but
 * with a self-contained, perceptually-uniform **OkLCH** tonal engine (no third-party color library)
 * and a guaranteed WCAG contrast pass so user-chosen colors can never produce illegible text.
 *
 * The scheme is assembled with the [lightColorScheme]/[darkColorScheme] factories (every role they
 * don't receive falls back to a sensible default) so it stays compatible across Material 3 versions.
 *
 * @param seed the user's chosen color.
 * @param dark whether to build the dark variant.
 * @param style how saturated the accents should be.
 * @param contrast 0f = standard (reading pairs ≥ 7:1 / general ≥ 4.5:1), 1f = high. Linearly raises
 *   the enforced minimums.
 */
internal fun colorSchemeFromSeed(
    seed: Color,
    dark: Boolean,
    style: SeedPaletteStyle = SeedPaletteStyle.TONAL_SPOT,
    contrast: Float = 0f,
): ColorScheme = buildSeedScheme(seed, dark, style, contrast) {}

/** Output of [seedScheme]: the scheme plus live-feedback signals for the custom-color UI. */
data class SeedSchemeReport(
    val scheme: ColorScheme,
    /** True if the WCAG clamp had to nudge at least one on-color away from its natural tone. */
    val adjusted: Boolean,
    /** Contrast of the primary accent against the surface — the main "is my color readable" signal. */
    val accentOnSurface: Float,
)

/** Like [colorSchemeFromSeed] but also reports whether the clamp intervened and the accent contrast. */
fun seedScheme(
    seed: Color,
    dark: Boolean,
    style: SeedPaletteStyle = SeedPaletteStyle.TONAL_SPOT,
    contrast: Float = 0f,
): SeedSchemeReport {
    var clamps = 0
    val scheme = buildSeedScheme(seed, dark, style, contrast) { clamps++ }
    return SeedSchemeReport(
        scheme = scheme,
        adjusted = clamps > 0,
        accentOnSurface = contrastRatio(scheme.primary, scheme.surface),
    )
}

private fun buildSeedScheme(
    seed: Color,
    dark: Boolean,
    style: SeedPaletteStyle,
    contrast: Float,
    onClamp: () -> Unit,
): ColorScheme {
    val seedLch = seed.toOkLch()
    val hue = seedLch.h
    val baseChroma = (seedLch.c * style.chromaScale).coerceIn(0.02f, 0.37f)

    val primary = TonalPalette(hue, baseChroma)
    val secondary = TonalPalette(hue, baseChroma * 0.34f)
    val tertiary = TonalPalette(hue + HUE_60_RAD, baseChroma * 0.62f)
    val neutral = TonalPalette(hue, minOf(baseChroma * 0.05f, 0.012f))
    val neutralVariant = TonalPalette(hue, minOf(baseChroma * 0.10f, 0.020f))

    // Reading pairs (the mushaf surface) are always held to at least AAA; the contrast setting only
    // raises floors, never lowers them.
    val readingTarget = lerp(7.0f, 10.5f, contrast)
    val generalTarget = lerp(4.5f, 7.0f, contrast)

    // Contrast clamp that also reports when it had to intervene.
    fun ens(fg: Color, bg: Color, target: Float): Color {
        val result = ensureContrast(fg, bg, target)
        if (result != fg) onClamp()
        return result
    }

    return if (dark) {
        val background = neutral.tone(6)
        val surface = neutral.tone(6)
        val surfaceVariant = neutralVariant.tone(30)
        val primaryContainer = primary.tone(30)
        val secondaryContainer = secondary.tone(30)
        val tertiaryContainer = tertiary.tone(30)
        val primaryColor = primary.tone(80)
        val secondaryColor = secondary.tone(80)
        val tertiaryColor = tertiary.tone(80)
        darkColorScheme(
            primary = primaryColor,
            onPrimary = ens(primary.tone(20), primaryColor, generalTarget),
            primaryContainer = primaryContainer,
            onPrimaryContainer = ens(primary.tone(90), primaryContainer, generalTarget),
            inversePrimary = primary.tone(40),
            secondary = secondaryColor,
            onSecondary = ens(secondary.tone(20), secondaryColor, generalTarget),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = ens(secondary.tone(90), secondaryContainer, generalTarget),
            tertiary = tertiaryColor,
            onTertiary = ens(tertiary.tone(20), tertiaryColor, generalTarget),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = ens(tertiary.tone(90), tertiaryContainer, generalTarget),
            error = Red80,
            onError = Red20,
            errorContainer = Red30,
            onErrorContainer = Red90,
            background = background,
            onBackground = ens(neutral.tone(90), background, readingTarget),
            surface = surface,
            onSurface = ens(neutral.tone(90), surface, readingTarget),
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = ens(neutralVariant.tone(80), surfaceVariant, generalTarget),
            surfaceContainerLowest = neutral.tone(4),
            surfaceContainerLow = neutral.tone(10),
            surfaceContainer = neutral.tone(12),
            surfaceContainerHigh = neutral.tone(17),
            surfaceContainerHighest = neutral.tone(22),
            inverseSurface = neutral.tone(90),
            inverseOnSurface = neutral.tone(20),
            outline = neutralVariant.tone(60),
            outlineVariant = neutralVariant.tone(30),
        )
    } else {
        val background = neutral.tone(98)
        val surface = neutral.tone(98)
        val surfaceVariant = neutralVariant.tone(90)
        val primaryContainer = primary.tone(90)
        val secondaryContainer = secondary.tone(90)
        val tertiaryContainer = tertiary.tone(90)
        val primaryColor = primary.tone(40)
        val secondaryColor = secondary.tone(40)
        val tertiaryColor = tertiary.tone(40)
        lightColorScheme(
            primary = primaryColor,
            onPrimary = ens(primary.tone(100), primaryColor, generalTarget),
            primaryContainer = primaryContainer,
            onPrimaryContainer = ens(primary.tone(10), primaryContainer, generalTarget),
            inversePrimary = primary.tone(80),
            secondary = secondaryColor,
            onSecondary = ens(secondary.tone(100), secondaryColor, generalTarget),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = ens(secondary.tone(10), secondaryContainer, generalTarget),
            tertiary = tertiaryColor,
            onTertiary = ens(tertiary.tone(100), tertiaryColor, generalTarget),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = ens(tertiary.tone(10), tertiaryContainer, generalTarget),
            error = Red40,
            onError = Color.White,
            errorContainer = Red90,
            onErrorContainer = Red10,
            background = background,
            onBackground = ens(neutral.tone(10), background, readingTarget),
            surface = surface,
            onSurface = ens(neutral.tone(10), surface, readingTarget),
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = ens(neutralVariant.tone(30), surfaceVariant, generalTarget),
            surfaceContainerLowest = neutral.tone(100),
            surfaceContainerLow = neutral.tone(96),
            surfaceContainer = neutral.tone(94),
            surfaceContainerHigh = neutral.tone(92),
            surfaceContainerHighest = neutral.tone(90),
            inverseSurface = neutral.tone(20),
            inverseOnSurface = neutral.tone(95),
            outline = neutralVariant.tone(50),
            outlineVariant = neutralVariant.tone(80),
        )
    }
}

/** A single Material-style tonal palette: a fixed hue & chroma, varied along the lightness (tone) axis. */
private class TonalPalette(private val hue: Float, private val chroma: Float) {
    /** [tone] is 0 (black) … 100 (white), mapped directly to OkLab lightness. */
    fun tone(tone: Int): Color = OkLch(tone / 100f, chroma, hue).toColor()
}

/** OkLCH color: perceptual lightness [l] (0..1), chroma [c], hue [h] in radians. */
internal data class OkLch(val l: Float, val c: Float, val h: Float)

private const val HUE_60_RAD = 1.0471976f

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)

private fun srgbToLinear(c: Float): Float =
    if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)

private fun linearToSrgb(c: Float): Float =
    if (c <= 0.0031308f) c * 12.92f else 1.055f * c.pow(1f / 2.4f) - 0.055f

/** sRGB [Color] → OkLCH. */
internal fun Color.toOkLch(): OkLch {
    val r = srgbToLinear(red)
    val g = srgbToLinear(green)
    val b = srgbToLinear(blue)
    val lp = cbrt(0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b)
    val mp = cbrt(0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b)
    val sp = cbrt(0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b)
    val okL = 0.2104542553f * lp + 0.7936177850f * mp - 0.0040720468f * sp
    val okA = 1.9779984951f * lp - 2.4285922050f * mp + 0.4505937099f * sp
    val okB = 0.0259040371f * lp + 0.7827717662f * mp - 0.8086757660f * sp
    return OkLch(okL, hypot(okA, okB), atan2(okB, okA))
}

/** OkLCH → sRGB [Color], clamped into the sRGB gamut. */
internal fun OkLch.toColor(): Color {
    val a = c * cos(h)
    val b = c * sin(h)
    val lp = l + 0.3963377774f * a + 0.2158037573f * b
    val mp = l - 0.1055613458f * a - 0.0638541728f * b
    val sp = l - 0.0894841775f * a - 1.2914855480f * b
    val l3 = lp * lp * lp
    val m3 = mp * mp * mp
    val s3 = sp * sp * sp
    val r = 4.0767416621f * l3 - 3.3077115913f * m3 + 0.2309699292f * s3
    val g = -1.2684380046f * l3 + 2.6097574011f * m3 - 0.3413193965f * s3
    val bl = -0.0041960863f * l3 - 0.7034186147f * m3 + 1.7076147010f * s3
    return Color(
        red = linearToSrgb(r).coerceIn(0f, 1f),
        green = linearToSrgb(g).coerceIn(0f, 1f),
        blue = linearToSrgb(bl).coerceIn(0f, 1f),
    )
}

/** WCAG 2.x contrast ratio (1..21) between two opaque colors. */
internal fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    val hi = maxOf(la, lb)
    val lo = minOf(la, lb)
    return (hi + 0.05f) / (lo + 0.05f)
}

/**
 * Returns [fg] unchanged if it already meets [target] contrast against [bg]; otherwise nudges it
 * along the OkLCH lightness axis toward whichever extreme (black/white) maximizes contrast, easing
 * chroma down so the result never looks muddy. Falls back to pure black/white in the worst case.
 */
internal fun ensureContrast(fg: Color, bg: Color, target: Float): Color {
    if (contrastRatio(fg, bg) >= target) return fg
    val lch = fg.toOkLch()
    val goDarker = contrastRatio(Color.Black, bg) >= contrastRatio(Color.White, bg)
    val steps = 48
    for (i in 1..steps) {
        val frac = i / steps.toFloat()
        val targetL = if (goDarker) lch.l * (1f - frac) else lch.l + (1f - lch.l) * frac
        val candidate = OkLch(targetL.coerceIn(0f, 1f), lch.c * (1f - frac), lch.h).toColor()
        if (contrastRatio(candidate, bg) >= target) return candidate
    }
    return if (goDarker) Color.Black else Color.White
}
