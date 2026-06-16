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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * A class to model gradient color values for Now in Android.
 *
 * @param top The top gradient color to be rendered.
 * @param bottom The bottom gradient color to be rendered.
 * @param container The container gradient color over which the gradient will be rendered.
 */
@Immutable
data class GradientColors(
    val top: Color = Color.Unspecified,
    val bottom: Color = Color.Unspecified,
    val container: Color = Color.Unspecified,
)

/**
 * A composition local for [GradientColors].
 */
val LocalGradientColors = staticCompositionLocalOf { GradientColors() }

/**
 * Corner colors for the decorative mesh gradient drawn on chrome surfaces. Already alpha-capped by
 * [SurauTheme] so the mesh stays a subtle wash; [Color.Unspecified] corners are treated as fully
 * transparent. Derived from the active color scheme so the mesh follows the user's theme.
 */
@Immutable
data class MeshGradientColors(
    val topStart: Color = Color.Unspecified,
    val topEnd: Color = Color.Unspecified,
    val bottomStart: Color = Color.Unspecified,
    val bottomEnd: Color = Color.Unspecified,
)

/** A composition local for [MeshGradientColors]. */
val LocalMeshGradientColors = staticCompositionLocalOf { MeshGradientColors() }

/**
 * Whether the decorative mesh gradient should actually render. Resolved by the app from the user
 * preference AND runtime gates (battery saver, reduced motion); `false` by default so chrome falls
 * back to the flat/linear background.
 */
val LocalMeshGradientEnabled = staticCompositionLocalOf { false }
