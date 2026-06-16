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

package org.surau.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Top-level navigation destinations that are owned by the app module (the composition root) rather
 * than by a dedicated feature module. The Quran tab keeps its own
 * [org.surau.app.feature.quran.api.navigation.QuranHomeNavKey].
 *
 * Home and Profile aggregate data across features, so they live here for now; Hadith and Kitabs are
 * "coming soon" placeholders. Any of these may graduate into a feature module later.
 */

/** Dashboard: reading streak, khatam progress, continue reading. The app's start destination. */
@Serializable
object HomeNavKey : NavKey

/** Hadith collections (placeholder — coming soon). */
@Serializable
object HadithNavKey : NavKey

/** Kitabs / books library (placeholder — coming soon). */
@Serializable
object KitabsNavKey : NavKey

/** Account + settings hub. */
@Serializable
object ProfileNavKey : NavKey
