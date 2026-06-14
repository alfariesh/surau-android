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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Light Surau color scheme — HeroUI Pro neutral sage palette with an olive-green accent.
 */
@VisibleForTesting
val LightSurauColorScheme = lightColorScheme(
    primary = HeroLightAccent,
    onPrimary = Color.White,
    primaryContainer = HeroLightPrimaryContainer,
    onPrimaryContainer = HeroLightOnPrimaryContainer,
    secondary = HeroLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = HeroLightSecondaryContainer,
    onSecondaryContainer = HeroLightOnSecondaryContainer,
    tertiary = HeroLightTertiary,
    onTertiary = Color.White,
    tertiaryContainer = HeroLightTertiaryContainer,
    onTertiaryContainer = HeroLightOnTertiaryContainer,
    error = Red40,
    onError = Color.White,
    errorContainer = Red90,
    onErrorContainer = Red10,
    background = HeroLightBackground,
    onBackground = HeroLightForeground,
    surface = HeroLightSurface,
    onSurface = HeroLightForeground,
    surfaceVariant = HeroLightSurfaceSecondary,
    onSurfaceVariant = HeroLightMuted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = HeroLightBackground,
    surfaceContainer = HeroLightSurfaceSecondary,
    surfaceContainerHigh = HeroLightSurfaceTertiary,
    surfaceContainerHighest = HeroLightSeparator,
    inverseSurface = HeroDarkBackground,
    inverseOnSurface = HeroLightBackground,
    inversePrimary = HeroDarkAccent,
    outline = HeroLightBorder,
    outlineVariant = HeroLightSeparator,
)

/**
 * Dark Surau color scheme — HeroUI Pro dark-olive palette with a lime-green accent.
 */
@VisibleForTesting
val DarkSurauColorScheme = darkColorScheme(
    primary = HeroDarkAccent,
    onPrimary = HeroDarkOnAccent,
    primaryContainer = HeroDarkPrimaryContainer,
    onPrimaryContainer = HeroDarkOnPrimaryContainer,
    secondary = HeroDarkSecondary,
    onSecondary = Color(0xFF26301E),
    secondaryContainer = HeroDarkSecondaryContainer,
    onSecondaryContainer = HeroDarkOnSecondaryContainer,
    tertiary = HeroDarkTertiary,
    onTertiary = Color(0xFF0E3438),
    tertiaryContainer = HeroDarkTertiaryContainer,
    onTertiaryContainer = HeroDarkOnTertiaryContainer,
    error = Red80,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90,
    background = HeroDarkBackground,
    onBackground = HeroDarkForeground,
    surface = HeroDarkSurface,
    onSurface = HeroDarkForeground,
    surfaceVariant = HeroDarkSurfaceSecondary,
    onSurfaceVariant = HeroDarkMuted,
    surfaceContainerLowest = HeroDarkBackground,
    surfaceContainerLow = HeroDarkSurface,
    surfaceContainer = HeroDarkSurfaceSecondary,
    surfaceContainerHigh = HeroDarkSurfaceTertiary,
    surfaceContainerHighest = HeroDarkSegment,
    inverseSurface = HeroLightBackground,
    inverseOnSurface = HeroDarkSurface,
    inversePrimary = HeroLightAccent,
    outline = HeroDarkBorder,
    outlineVariant = HeroDarkSeparator,
)

/**
 * Surau theme.
 *
 * Material 3 Expressive theme with the Surau emerald & gold color schemes.
 *
 * @param darkTheme Whether the theme should use a dark color scheme (follows system by default).
 * @param disableDynamicTheming If `true`, disables the use of dynamic theming, even when it is
 *        supported.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SurauTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    disableDynamicTheming: Boolean = true,
    content: @Composable () -> Unit,
) {
    // Color scheme
    val colorScheme = when {
        !disableDynamicTheming && supportsDynamicTheming() -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

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
        !disableDynamicTheming && supportsDynamicTheming() -> emptyGradientColors
        else -> defaultGradientColors
    }
    // Background theme
    val backgroundTheme = BackgroundTheme(
        color = colorScheme.surface,
        tonalElevation = 2.dp,
    )
    val tintTheme = when {
        !disableDynamicTheming && supportsDynamicTheming() -> TintTheme(colorScheme.primary)
        else -> TintTheme()
    }
    // Extended HeroUI semantic tokens: static by default, derived from the dynamic scheme when on.
    val semanticColors = when {
        !disableDynamicTheming && supportsDynamicTheming() ->
            surauSemanticColorsFromScheme(colorScheme, darkTheme)

        else -> if (darkTheme) DarkSurauSemanticColors else LightSurauSemanticColors
    }
    // Composition locals
    CompositionLocalProvider(
        LocalGradientColors provides gradientColors,
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
