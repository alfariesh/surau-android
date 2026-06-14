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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.surau.app.core.designsystem.theme.SurauTheme

/**
 * Tag/collection palette. Colours are used only for the leading dot and a subtle container tint,
 * so they read clearly on both light and dark surfaces without contrast tuning.
 */
private val TagPalette = listOf(
    Color(0xFF1E8E5A), // emerald
    Color(0xFF0E7C86), // teal
    Color(0xFF1976D2), // blue
    Color(0xFF7E57C2), // violet
    Color(0xFFC2185B), // rose
    Color(0xFFEF6C00), // orange
    Color(0xFF6D4C41), // brown
    Color(0xFFB8860B), // goldenrod
)

/**
 * Fixed colours for the well-known preset collections, so they always look the same. Keys are
 * lower-cased tag values; see `feature_quran_impl_bookmark_collections`.
 */
private val NamedTagColors = mapOf(
    "hafalan" to Color(0xFF1E8E5A),
    "doa" to Color(0xFFB8860B),
    "favorit" to Color(0xFFC2185B),
    "renungan" to Color(0xFF0E7C86),
    "tadabbur" to Color(0xFF7E57C2),
)

/**
 * Deterministic colour for a tag/collection label: a fixed colour for known presets, otherwise a
 * stable choice from [TagPalette] derived from the label. The same label is always the same colour
 * everywhere (filter chips, list rows, editor) with no per-tag storage, so tags still sync to the
 * backend as plain strings.
 */
fun tagColor(label: String): Color {
    val key = label.trim().lowercase()
    return NamedTagColors[key] ?: TagPalette[key.hashCode().mod(TagPalette.size)]
}

/**
 * A small filled circle in the tag's colour, for use as the leading icon of a Material chip.
 */
@Composable
fun TagDot(label: String, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(tagColor(label)),
    )
}

/**
 * A read-only, colour-coded tag chip (coloured dot + label on a tinted container). Use [TagDot]
 * directly when decorating an interactive Material chip instead.
 */
@Composable
fun TagChip(label: String, modifier: Modifier = Modifier) {
    val color = tagColor(label)
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TagDot(label)
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview
@Composable
private fun TagChipPreview() {
    SurauTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(8.dp)) {
            TagChip("Hafalan")
            TagChip("Doa")
            TagChip("tadabbur")
        }
    }
}
