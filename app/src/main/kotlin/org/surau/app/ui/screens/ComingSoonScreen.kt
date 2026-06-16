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

package org.surau.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.surau.app.core.designsystem.component.BookCoverflow
import org.surau.app.core.designsystem.component.CoverflowBook
import org.surau.app.core.designsystem.icon.SurauIcons
import org.surau.app.core.designsystem.theme.SurauTheme

/** A decorative book cover shown in the [ComingSoonScreen] book showcase. */
data class ComingSoonBook(
    val title: String,
    val color: Color,
    val textColor: Color = Color(0xFFF8FAFC),
)

/**
 * A friendly placeholder for sections that are planned but not yet built (Hadith, Kitabs).
 *
 * @param heading the 24sp "coming soon" headline, e.g. "Segera Hadir".
 * @param body an informative line telling the user what's happening (content being prepared, feature
 *   being finalised) and what to do next.
 * @param icon the section's glyph, shown when [books] is empty.
 * @param books optional preview covers; when present they replace the glyph with an animated
 *   [BookCoverflow] (tap a cover to see it open).
 * @param actionLabel optional CTA label; when set, a rounded "Join Beta" button is shown.
 * @param onActionClick invoked when the CTA button is tapped.
 * @param socialText optional social-proof line shown beside an avatar group below the CTA.
 */
@Composable
fun ComingSoonScreen(
    heading: String,
    body: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    books: List<ComingSoonBook> = emptyList(),
    actionLabel: String? = null,
    onActionClick: () -> Unit = {},
    socialText: String? = null,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // The book marquee bleeds edge-to-edge (no horizontal inset) so the covers — and
            // their drop shadows — run the full width. The textual content below stays inset.
            if (books.isEmpty()) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(SurauTheme.colors.accentSoft, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = SurauTheme.colors.accent,
                        modifier = Modifier.size(44.dp),
                    )
                }
            } else {
                BookShowcase(books = books)
            }
            Column(
                modifier = Modifier.padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 24sp "coming soon" headline (headlineSmall is 24sp SemiBold).
                Text(
                    text = heading,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = SurauTheme.colors.muted,
                    textAlign = TextAlign.Center,
                )
                if (actionLabel != null || socialText != null) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        if (actionLabel != null) {
                            JoinBetaButton(label = actionLabel, onClick = onActionClick)
                        }
                        if (socialText != null) {
                            SocialProof(text = socialText)
                        }
                    }
                }
            }
        }
    }
}

/**
 * A looping coverflow marquee of book covers: the cover crossing the centre grows
 * and opens (the GeistBook open effect), neighbours taper to the sides, and the
 * left/right edges fade out.
 */
@Composable
private fun BookShowcase(books: List<ComingSoonBook>) {
    BookCoverflow(
        books = books.map { CoverflowBook(it.title, it.color, it.textColor) },
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Rounded-full accent CTA with a trailing arrow — the "Join Beta Access" button. */
@Composable
private fun JoinBetaButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = SurauTheme.colors.accent,
            contentColor = SurauTheme.colors.onAccent,
        ),
        contentPadding = ButtonDefaults.SmallContentPadding,
    ) {
        Text(text = label, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = SurauIcons.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** A decorative social-proof face: initials over a soft tinted disc. */
private data class SocialFace(
    val initials: String,
    val container: Color,
    val content: Color,
)

/** An overlapping avatar group plus a muted social-proof line. */
@Composable
private fun SocialProof(text: String) {
    val faces = listOf(
        SocialFace("SA", SurauTheme.colors.accentSoft, SurauTheme.colors.accent),
        SocialFace("NF", SurauTheme.colors.successContainer, SurauTheme.colors.success),
        SocialFace("RH", SurauTheme.colors.warningContainer, SurauTheme.colors.warning),
        SocialFace("ZK", SurauTheme.colors.dangerContainer, SurauTheme.colors.danger),
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Negative spacing overlaps the discs; each later disc draws on top, and its
        // surface-coloured ring separates it from the one behind (web `-space-x-2`).
        Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
            faces.forEach { face -> AvatarDisc(face) }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = SurauTheme.colors.muted,
        )
    }
}

@Composable
private fun AvatarDisc(face: SocialFace) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .border(2.dp, SurauTheme.colors.surface, CircleShape)
            .clip(CircleShape)
            .background(face.container),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = face.initials,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = face.content,
        )
    }
}

@Preview
@Composable
private fun ComingSoonScreenPreview() {
    SurauTheme {
        ComingSoonScreen(
            heading = "Segera Hadir",
            body = "Kami sedang menyiapkan koleksi Hadis dan mematangkan fiturnya agar " +
                "nyaman dibaca. Daftar untuk jadi yang pertama tahu saat rilis.",
            icon = SurauIcons.AutoStories,
            books = listOf(
                ComingSoonBook("Sahih al-Bukhari", Color(0xFF0F766E)),
                ComingSoonBook("Sahih Muslim", Color(0xFFB45309)),
                ComingSoonBook("Sunan Abu Dawud", Color(0xFF4338CA)),
            ),
            actionLabel = "Gabung Akses Beta",
            socialText = "1.200+ pembaca sudah bergabung",
        )
    }
}
