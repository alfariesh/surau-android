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

package org.surau.app.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import org.surau.app.core.designsystem.theme.SurauTheme
import org.surau.app.core.designsystem.theme.quranTextStyle

/**
 * Renders Quran ayah text (QPC Hafs encoding) with the bundled KFGQPC Uthmanic HAFS font in a
 * right-to-left context.
 *
 * @param text the ayah text from the backend's `text_qpc_hafs` field.
 * @param fontScale the user-selected Arabic font scale (1.0 = default).
 */
@Composable
fun AyahText(
    text: String,
    modifier: Modifier = Modifier,
    fontScale: Float = 1f,
    color: Color = Color.Unspecified,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text = text,
            style = quranTextStyle(fontScale),
            color = color,
            modifier = modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AyahTextPreview() {
    SurauTheme {
        AyahText(
            text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
