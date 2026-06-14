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

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.surau.app.core.designsystem.R

/**
 * KFGQPC Uthmanic Script HAFS (v22) — the official King Fahd Glorious Quran Printing Complex
 * typeface for Unicode Hafs Quran text. The backend's `text_qpc_hafs` field is authored for this
 * font; system fallbacks misrender waqf marks and small letters, so it must always be used for
 * ayah text.
 */
val UthmanicHafsFontFamily = FontFamily(
    Font(R.font.core_designsystem_uthmanic_hafs),
)

/**
 * Surah Name (v4) — a decorative glyph font where each surah's calligraphic name is encoded as a
 * standard ligature. Rendering the literal token `surahNNN` (zero-padded surah id, see
 * [surahNameGlyphCode]) makes the font's `liga` feature substitute the single ornament glyph.
 * Always pair it with [surahNameGlyphCode]; the raw Arabic name will not trigger the ligature.
 */
val SurahNameFontFamily = FontFamily(
    Font(R.font.core_designsystem_surah_name),
)

/**
 * The ligature token that [SurahNameFontFamily] substitutes for surah [surahId]'s ornament glyph,
 * e.g. `surahNameGlyphCode(2)` -> `"surah002"`.
 */
fun surahNameGlyphCode(surahId: Int): String = "surah%03d".format(surahId)

/** Base font size for ayah text before the user's Arabic font scale is applied. */
val QURAN_BASE_FONT_SIZE = 24

/**
 * Text style for Quran ayah text.
 *
 * @param fontScale the user-selected Arabic font scale (1.0 = default).
 */
fun quranTextStyle(fontScale: Float = 1f): TextStyle = TextStyle(
    fontFamily = UthmanicHafsFontFamily,
    fontSize = (QURAN_BASE_FONT_SIZE * fontScale).sp,
    // Generous line height keeps stacked harakat and waqf marks from clipping.
    lineHeight = 2.0.em,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
    textDirection = TextDirection.Rtl,
    localeList = LocaleList("ar"),
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)
