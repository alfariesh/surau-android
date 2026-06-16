/*
 * Copyright 2022 The Android Open Source Project
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

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * The app's baseline light/dark color schemes — the hand-tuned HeroUI **Default** palette (balanced
 * neutral grey with a blue accent). Defined in HeroPalettes.kt; alternative palettes (Mouve, Sky)
 * live alongside it and are selectable via [HeroPalette].
 */
@VisibleForTesting
val LightSurauColorScheme = DefaultLightColorScheme

@VisibleForTesting
val DarkSurauColorScheme = DefaultDarkColorScheme

/**
 * Surau theme.
 *
 * Material 3 Expressive theme with the Surau emerald & gold color schemes.
 *
 * @param darkTheme Whether the theme should use a dark color scheme (follows system by default).
 * @param disableDynamicTheming If `true`, disables the use of dynamic theming, even when it is
 *        supported.
 * @param seedColor A user-chosen seed color. When specified (and dynamic theming is off), the whole
 *        scheme is generated from it via [colorSchemeFromSeed]; [Color.Unspecified] keeps the default
 *        HeroUI Pro (Zamrud) scheme so existing call sites and goldens are unaffected.
 * @param seedStyle How vivid the generated scheme's accents are.
 * @param seedContrast Extra contrast floor for the generated scheme (0f = standard, 1f = high).
 * @param meshGradientEnabled Whether the decorative mesh gradient should render on chrome. Resolved
 *        by the caller from the user preference and runtime gates (battery saver, reduced motion).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SurauTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    disableDynamicTheming: Boolean = true,
    seedColor: Color = Color.Unspecified,
    seedStyle: SeedPaletteStyle = SeedPaletteStyle.TONAL_SPOT,
    seedContrast: Float = 0f,
    meshGradientEnabled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val useDynamic = !disableDynamicTheming && supportsDynamicTheming()
    val useSeed = !useDynamic && seedColor != Color.Unspecified
    // Color scheme
    val colorScheme = when {
        useDynamic -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        useSeed -> colorSchemeFromSeed(seedColor, darkTheme, seedStyle, seedContrast)

        else -> if (darkTheme) DarkSurauColorScheme else LightSurauColorScheme
    }
    // Gradient colors
    val emptyGradientColors = GradientColors(container = colorScheme.surfaceColorAtElevation(2.dp))
    val defaultGradientColors = GradientColors(
        top = colorScheme.inverseOnSurface,
        bottom = colorScheme.primaryContainer,
        container = colorScheme.surface,
    )
    val gradientColors = when {
        useDynamic -> emptyGradientColors
        else -> defaultGradientColors
    }
    // Background theme — the app canvas uses the `background` token (page color), not `surface`.
    val backgroundTheme = BackgroundTheme(
        color = colorScheme.background,
        tonalElevation = 0.dp,
    )
    val tintTheme = when {
        useDynamic -> TintTheme(colorScheme.primary)
        else -> TintTheme()
    }
    // Extended HeroUI semantic tokens: static by default, derived from the live scheme for both the
    // dynamic (wallpaper) and the user-seed paths so every component follows the chosen color.
    val semanticColors = when {
        useDynamic || useSeed -> surauSemanticColorsFromScheme(colorScheme, darkTheme)
        else -> if (darkTheme) DarkSurauSemanticColors else LightSurauSemanticColors
    }
    // Decorative mesh corners: a top band of primary→tertiary containers fading to transparent,
    // alpha-capped so it stays a subtle wash. Empty on the wallpaper-dynamic path (Material You owns
    // its own look there).
    val meshAlpha = if (darkTheme) 0.08f else 0.12f
    val meshGradientColors = if (useDynamic) {
        MeshGradientColors()
    } else {
        MeshGradientColors(
            topStart = colorScheme.primaryContainer.copy(alpha = meshAlpha),
            topEnd = colorScheme.tertiaryContainer.copy(alpha = meshAlpha),
            bottomStart = Color.Unspecified,
            bottomEnd = Color.Unspecified,
        )
    }
    // Composition locals
    CompositionLocalProvider(
        LocalGradientColors provides gradientColors,
        LocalMeshGradientColors provides meshGradientColors,
        LocalMeshGradientEnabled provides meshGradientEnabled,
        LocalBackgroundTheme provides backgroundTheme,
        LocalTintTheme provides tintTheme,
        LocalSurauColors provides semanticColors,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = SurauTypography,
            content = content,
        )
    }
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
fun supportsDynamicTheming() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
