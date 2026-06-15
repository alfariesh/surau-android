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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sign

/** A book shown in a [BookCoverflow]. */
data class CoverflowBook(
    val title: String,
    val color: Color,
    val textColor: Color = Color(0xFFF8FAFC),
)

/**
 * A center-focus coverflow marquee of [GeistBook] covers — the Android port of the
 * web reader's "coming soon" coverflow. The row loops right → left forever; the
 * cover crossing the centre grows to [maxScale] and opens from its spine (the
 * GeistBook open effect), with neighbours pushed outward so the enlarged centre
 * book keeps a gap. The left/right edges fade out.
 *
 * Every per-frame transform (marquee offset, scale, open, spread) is read inside
 * a graphicsLayer block, so the animation runs without recomposition.
 */
@Composable
fun BookCoverflow(
    books: List<CoverflowBook>,
    modifier: Modifier = Modifier,
    bookWidth: Dp = 88.dp,
    gap: Dp = 16.dp,
    height: Dp = 200.dp,
    /** Marquee speed, in dp travelled per second (right → left). */
    speed: Dp = 38.dp,
    /** Scale of the centre book (1.25 = +25%). */
    maxScale: Float = 1.25f,
    /** How far neighbours are pushed outward so the centre book keeps a gap. */
    spread: Dp = 22.dp,
    /** Width of the left/right edge fade. */
    edgeFade: Dp = 44.dp,
) {
    if (books.isEmpty()) return

    val density = LocalDensity.current
    val bookWidthPx = with(density) { bookWidth.toPx() }
    val itemWidthPx = with(density) { (bookWidth + gap).toPx() }
    val spreadPx = with(density) { spread.toPx() }
    val edgeFadePx = with(density) { edgeFade.toPx() }
    val speedPxPerSec = with(density) { speed.toPx() }
    val falloffPx = itemWidthPx * 2f
    val count = books.size

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .horizontalFade(edgeFadePx),
    ) {
        val viewportPx = constraints.maxWidth.toFloat()
        val viewportCenter = viewportPx / 2f
        val unitWidthPx = count * itemWidthPx
        // Enough repetitions that the viewport always stays filled while looping.
        val copies = (ceil(((viewportPx + itemWidthPx) / unitWidthPx).toDouble()).toInt() + 1)
            .coerceAtLeast(3)
        val total = count * copies
        val durationMillis = ((unitWidthPx / speedPxPerSec) * 1000f).roundToInt().coerceAtLeast(1)

        val transition = rememberInfiniteTransition(label = "coverflow")
        // Advancing this by one "unit" (one full pass of the book list) and
        // restarting is seamless: every item is identical to the one a unit back.
        val offset by transition.animateFloat(
            initialValue = 0f,
            targetValue = unitWidthPx,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = durationMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "offset",
        )

        repeat(total) { index ->
            val book = books[index % count]
            // Focus = proximity to the viewport centre, read live per frame.
            val focusOf: () -> Float = {
                val centerX = index * itemWidthPx - offset + bookWidthPx / 2f
                1f - smoothstep((abs(centerX - viewportCenter) / falloffPx).coerceIn(0f, 1f))
            }

            GeistBook(
                title = book.title,
                color = book.color,
                textColor = book.textColor,
                variant = GeistBookVariant.Simple,
                ratio = GeistBookRatio.Geist,
                width = bookWidth,
                // Empty illustration → a clean, text-only cover.
                illustration = {},
                openProgress = focusOf,
                // BoxWithConstraints is a Box, so every cover stacks at the same
                // CenterStart anchor; translationX then fans them across the row and
                // scale grows around each cover's own centre.
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .graphicsLayer {
                        val centerX = index * itemWidthPx - offset + bookWidthPx / 2f
                        val d = centerX - viewportCenter
                        val focus = 1f - smoothstep((abs(d) / falloffPx).coerceIn(0f, 1f))
                        // Dock-style spread: push neighbours out so the enlarged centre keeps a gap.
                        val push = sign(d) * spreadPx * smoothstep((abs(d) / itemWidthPx).coerceIn(0f, 1f))
                        translationX = (index * itemWidthPx - offset) + push
                        val s = 1f + (maxScale - 1f) * focus
                        scaleX = s
                        scaleY = s
                    },
            )
        }
    }
}

// Cubic smoothstep — 0 at the edges, eased in/out, 1 at t=1.
private fun smoothstep(value: Float): Float {
    val c = value.coerceIn(0f, 1f)
    return c * c * (3f - 2f * c)
}

/** Fades the left and right [fadePx] of the content to transparent. */
private fun Modifier.horizontalFade(fadePx: Float): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        if (fadePx <= 0f || size.width <= 0f) return@drawWithContent
        val frac = (fadePx / size.width).coerceIn(0f, 0.5f)
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent,
                frac to Color.Black,
                1f - frac to Color.Black,
                1f to Color.Transparent,
            ),
            blendMode = BlendMode.DstIn,
        )
    }
