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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Extended HeroUI Pro semantic colors that Material 3's [ColorScheme] has no slot for.
 *
 * These complement [androidx.compose.material3.MaterialTheme.colorScheme] (which carries the
 * standard M3 roles) with the additional tokens the Surau components need — interactive neutral
 * surfaces, field chrome, separators, the segmented-control track, and the toast status families.
 *
 * Read them inside a composable via [LocalSurauColors] (e.g. `LocalSurauColors.current.separator`).
 * Provided by [SurauTheme]; when dynamic theming is active they are derived from the live
 * [ColorScheme] by [surauSemanticColorsFromScheme] so components still adapt to the wallpaper.
 */
@Immutable
data class SurauSemanticColors(
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    /** Neutral interactive background — switch-off track, segmented track base, subtle chips. */
    val default: Color,
    /** The app canvas / page background — the lowest layer, beneath surfaces and cards. */
    val background: Color,
    val surface: Color,
    val surfaceSecondary: Color,
    val surfaceTertiary: Color,
    val surfaceHover: Color,
    val overlay: Color,
    val fieldBackground: Color,
    val fieldBorder: Color,
    val border: Color,
    val separator: Color,
    val muted: Color,
    val scrollbar: Color,
    /** HeroUI `--segment`: the raised sliding indicator fill for tabs (primary) and segmented control. */
    val segment: Color,
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val danger: Color,
    val onDanger: Color,
    val dangerContainer: Color,
)

/**
 * The app's baseline extended semantic colors — the hand-tuned HeroUI **Default** palette (balanced
 * neutral grey with a blue accent). Defined in HeroPalettes.kt; the seed/dynamic paths still derive
 * their own from the live scheme via [surauSemanticColorsFromScheme].
 */
internal val LightSurauSemanticColors = DefaultLightSemanticColors

internal val DarkSurauSemanticColors = DefaultDarkSemanticColors

/**
 * Derives [SurauSemanticColors] from a Material 3 [ColorScheme] so the extended tokens follow a
 * dynamic (wallpaper) color scheme on Android 12+. Used by [SurauTheme] when dynamic theming is on.
 */
internal fun surauSemanticColorsFromScheme(
    colorScheme: ColorScheme,
    darkTheme: Boolean,
): SurauSemanticColors {
    val base = if (darkTheme) DarkSurauSemanticColors else LightSurauSemanticColors
    return base.copy(
        accent = colorScheme.primary,
        onAccent = colorScheme.onPrimary,
        accentSoft = colorScheme.primaryContainer,
        default = colorScheme.surfaceContainerHigh,
        background = colorScheme.background,
        surface = colorScheme.surface,
        surfaceSecondary = colorScheme.surfaceContainer,
        surfaceTertiary = colorScheme.surfaceContainerHighest,
        surfaceHover = colorScheme.surfaceContainerHigh,
        overlay = colorScheme.surfaceContainerLow,
        fieldBackground = colorScheme.surface,
        fieldBorder = colorScheme.outline,
        border = colorScheme.outline,
        separator = colorScheme.outlineVariant,
        muted = colorScheme.onSurfaceVariant,
        segment = colorScheme.surfaceContainerLowest,
    )
}

/**
 * [androidx.compose.runtime.CompositionLocal] carrying the extended Surau semantic colors. Defaults
 * to the light tokens; [SurauTheme] overrides it with the resolved (static or dynamic) instance.
 */
val LocalSurauColors = staticCompositionLocalOf { LightSurauSemanticColors }

/**
 * Convenience accessor for the extended Surau semantic colors, mirroring
 * `MaterialTheme.colorScheme`. Use as `SurauTheme.colors.separator`.
 */
object SurauTheme {
    val colors: SurauSemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSurauColors.current
}
