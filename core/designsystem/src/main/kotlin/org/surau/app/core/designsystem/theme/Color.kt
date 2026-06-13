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

import androidx.compose.ui.graphics.Color

/**
 * Surau colors.
 *
 * Tonal palettes derived from an emerald (#006D4A) seed with a warm gold secondary and a
 * deep teal tertiary — a classic Islamic green & gold pairing.
 */
internal val Emerald10 = Color(0xFF002114)
internal val Emerald20 = Color(0xFF003824)
internal val Emerald30 = Color(0xFF005236)
internal val Emerald40 = Color(0xFF006D4A)
internal val Emerald80 = Color(0xFF58DBA4)
internal val Emerald90 = Color(0xFF77F8BE)

internal val Gold10 = Color(0xFF261A00)
internal val Gold20 = Color(0xFF402D00)
internal val Gold30 = Color(0xFF5C4200)
internal val Gold40 = Color(0xFF785900)
internal val Gold80 = Color(0xFFF2BF42)
internal val Gold90 = Color(0xFFFFDF99)

internal val Teal10 = Color(0xFF001F24)
internal val Teal20 = Color(0xFF00363D)
internal val Teal30 = Color(0xFF004F58)
internal val Teal40 = Color(0xFF006874)
internal val Teal80 = Color(0xFF4FD8EB)
internal val Teal90 = Color(0xFF97F0FF)

internal val Red10 = Color(0xFF410002)
internal val Red20 = Color(0xFF690005)
internal val Red30 = Color(0xFF93000A)
internal val Red40 = Color(0xFFBA1A1A)
internal val Red80 = Color(0xFFFFB4AB)
internal val Red90 = Color(0xFFFFDAD6)

internal val GreenGray30 = Color(0xFF404943)
internal val GreenGray50 = Color(0xFF707973)
internal val GreenGray60 = Color(0xFF8A938C)
internal val GreenGray80 = Color(0xFFC0C9C1)
internal val GreenGray90 = Color(0xFFDCE5DD)

internal val DarkGreenGray10 = Color(0xFF191C1A)
internal val DarkGreenGray20 = Color(0xFF2E312E)
internal val DarkGreenGray90 = Color(0xFFE1E3DF)
internal val DarkGreenGray95 = Color(0xFFEFF1ED)
internal val DarkGreenGray99 = Color(0xFFFBFDF8)

/**
 * HeroUI Pro tokens, converted from the source `oklch(...)` definitions to sRGB.
 *
 * A neutral sage/olive palette: warm off-white (light) / dark-olive (dark) surfaces with a single
 * olive-green accent. These drive the **default** [LightSurauColorScheme] / [DarkSurauColorScheme]
 * and the extended [SurauSemanticColors] tokens that Material 3 has no slot for.
 *
 * The few roles HeroUI Pro leaves unspecified (containers, secondary/tertiary, foregrounds, the
 * success/warning families) are derived in the same `oklch` space so they stay tonally consistent.
 */
// Light
internal val HeroLightBackground = Color(0xFFEFF1ED)
internal val HeroLightSurface = Color(0xFFFBFCFA) // color-mix(white 75%, transparent) over background
internal val HeroLightSurfaceGlass = Color(0xBFFFFFFF) // translucent white @ 75% for glassy surfaces
internal val HeroLightSurfaceSecondary = Color(0xFFEFEFEF)
internal val HeroLightSurfaceTertiary = Color(0xFFEAEAEA)
internal val HeroLightDefault = Color(0xFFE5E9E2)
internal val HeroLightAccent = Color(0xFF4F772D)
internal val HeroLightMuted = Color(0xFF727272)
internal val HeroLightScrollbar = Color(0xFFD4D4D4)
internal val HeroLightBorder = Color(0xFFDEDEDE)
internal val HeroLightSeparator = Color(0xFFE4E4E4)
internal val HeroLightForeground = Color(0xFF1E201D)
internal val HeroLightPrimaryContainer = Color(0xFFD0E6C1)
internal val HeroLightOnPrimaryContainer = Color(0xFF123200)
internal val HeroLightSecondary = Color(0xFF535B4D)
internal val HeroLightSecondaryContainer = Color(0xFFD9E1D4)
internal val HeroLightOnSecondaryContainer = Color(0xFF1A2213)
internal val HeroLightTertiary = Color(0xFF4A6367)
internal val HeroLightTertiaryContainer = Color(0xFFD0E2E5)
internal val HeroLightOnTertiaryContainer = Color(0xFF001F25)
internal val HeroLightSegment = Color(0xFFE5E9E2)
internal val HeroLightSuccess = Color(0xFF357A3A)
internal val HeroLightSuccessSoft = Color(0xFFD1EED1)
internal val HeroLightWarning = Color(0xFFB37903)
internal val HeroLightWarningSoft = Color(0xFFFAE1B8)

// Dark
internal val HeroDarkBackground = Color(0xFF15170C)
internal val HeroDarkSurface = Color(0xFF181818)
internal val HeroDarkSurfaceSecondary = Color(0xFF232323)
internal val HeroDarkSurfaceTertiary = Color(0xFF272727)
internal val HeroDarkOverlay = Color(0xFF181818)
internal val HeroDarkDefault = Color(0xFF272727)
internal val HeroDarkAccent = Color(0xFF90A955)
internal val HeroDarkMuted = Color(0xFFA0A0A0)
internal val HeroDarkScrollbar = Color(0xFFA0A0A0)
internal val HeroDarkFieldBackground = Color(0xFF181818)
internal val HeroDarkFieldBorder = Color(0xFF292929)
internal val HeroDarkBorder = Color(0xFF292929)
internal val HeroDarkSeparator = Color(0xFF222222)
internal val HeroDarkSegment = Color(0xFF474747)
internal val HeroDarkForeground = Color(0xFFE4E5E2)
internal val HeroDarkOnAccent = Color(0xFF111308)
internal val HeroDarkPrimaryContainer = Color(0xFF364315)
internal val HeroDarkOnPrimaryContainer = Color(0xFFD1E99F)
internal val HeroDarkSecondary = Color(0xFFB2BAAD)
internal val HeroDarkSecondaryContainer = Color(0xFF2A3026)
internal val HeroDarkOnSecondaryContainer = Color(0xFFD9E1D4)
internal val HeroDarkTertiary = Color(0xFFA8C3C8)
internal val HeroDarkTertiaryContainer = Color(0xFF1B3236)
internal val HeroDarkOnTertiaryContainer = Color(0xFFC8E4E9)
internal val HeroDarkSuccess = Color(0xFF6DBA70)
internal val HeroDarkSuccessSoft = Color(0xFF18361A)
internal val HeroDarkWarning = Color(0xFFE9B452)
internal val HeroDarkWarningSoft = Color(0xFF3F2903)
