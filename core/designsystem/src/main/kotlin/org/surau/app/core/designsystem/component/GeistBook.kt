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

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Geist Book component — Android Compose port of
 * geist-main/src/lib/components/ui/book/book.svelte
 *
 * A stylised 3D-look book that tilts on press to reveal its spine, pages and
 * back cover. Supports left-to-right (default) and right-to-left (for Arabic
 * books where the spine is on the right).
 */

enum class GeistBookVariant {
    /** Colored top stripe + body-background bottom half with title + Vercel logo. */
    Stripe,

    /** Full-color body with title + SimpleIllustration (or custom). */
    Simple,

    /** Full-bleed [illustration] covers the whole front; title is overlaid with a
     *  gradient scrim for legibility. */
    Image,
}

/**
 * Preset aspect ratios. `w:h` defines the book's width-to-height ratio.
 * Height in composition = `width × h / w`.
 */
enum class GeistBookRatio(val w: Float, val h: Float) {
    /** Vercel Geist original (49 : 60). */
    Geist(49f, 60f),

    /** ISO A-series / B-series (1 : √2). */
    A4(210f, 297f),

    /** ISO B5. */
    B5(176f, 250f),

    /** Common US novel / mass-market paperback (5 : 7). */
    Novel(5f, 7f),

    /** US trade paperback (6 : 9). */
    TradePaperback(6f, 9f),

    /** Square (1 : 1). */
    Square(1f, 1f),
}

val GeistAmber600 = Color(0xFFF7B955)
val GeistBackground200Light = Color(0xFFFAFAFA)

/**
 * Design tokens mirrored from the Svelte reference. Keep them named so the
 * ratios stay readable when tweaking visuals.
 */
private object GeistBookTokens {
    // Book "thickness" — Svelte: 29cqw (29% of book width)
    const val DEPTH_RATIO = 0.29f

    // Projected horizontal peek of the back cover when tilted ≈ depth·sin(|tilt|).
    // We use a slightly bumped value so the depth reads clearly in 2D.
    const val BACK_REVEAL_RATIO = 0.34f

    // Pages slab width sits between the front and back, filling most of the gap.
    const val PAGES_VISIBLE_RATIO = 0.28f

    // Pages' right-side foreshortening (taper amount as fraction of book height).
    const val PAGES_TAPER_RATIO = 0.018f

    // Spine shadow band covers the left/right 8.2% of the cover.
    const val SPINE_WIDTH_RATIO = 0.082f

    // Body padding: 6.1% on three sides, 14.2% on the spine side.
    const val BODY_PADDING_RATIO = 0.061f
    const val BODY_PADDING_SPINE_RATIO = 0.142f

    // Title typography, relative to book width.
    const val TITLE_SIZE_RATIO = 0.105f
    const val TITLE_LINE_HEIGHT_MUL = 1.25f
    const val TITLE_LETTER_SPACING_MUL = -0.02f

    // Vercel logo / simple illustration sizes, relative to book width.
    const val VERCEL_LOGO_RATIO = 0.082f
    const val SIMPLE_ILLUSTRATION_RATIO = 0.22f

    // Cover corners: larger on the spine side, smaller on the outer edge.
    val SpineCorner = 6.dp
    val OuterCorner = 4.dp

    // Press interaction.
    const val PRESS_TILT_DEGREES = -20f
    const val PRESS_SCALE = 1.03f
    const val CAMERA_DISTANCE_MUL = 12f

    // Back cover is the same hue as the front but this much darker.
    const val BACK_DARKEN_FRACTION = 0.35f

    // Front-cover drop shadow elevation.
    val ShadowElevation = 8.dp
}

/**
 * A stylised 3D book card.
 *
 * @param title Text printed on the cover.
 * @param color Primary cover colour (stripe top in Stripe variant, full cover in Simple).
 * @param textColor Colour of the title and illustration.
 * @param variant Stripe (colored top half + body bottom) or Simple (full-color body).
 * @param textured If true, a subtle noise overlay is blended onto the cover.
 * @param width Book width. Height is derived from the 49:60 aspect ratio.
 * @param bodyBackground Paper colour used for the Stripe variant's bottom half.
 * @param onClick Invoked when the user lifts their finger after tapping.
 * @param rtl Mirrors the book for right-to-left (Arabic) layout: spine and tilt flip to the right.
 * @param illustration Optional custom illustration. Stripe places it in the stripe area;
 *                     Simple replaces the default SimpleIllustration.
 */
@Composable
fun GeistBook(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = GeistAmber600,
    textColor: Color = Color(0xFF171717),
    variant: GeistBookVariant = GeistBookVariant.Stripe,
    textured: Boolean = false,
    width: Dp = 196.dp,
    ratio: GeistBookRatio = GeistBookRatio.B5,
    bodyBackground: Color = GeistBackground200Light,
    onClick: (() -> Unit)? = null,
    rtl: Boolean = false,
    illustration: (@Composable () -> Unit)? = null,
    openProgress: (() -> Float)? = null,
) {
    val density = LocalDensity.current
    val widthPx = with(density) { width.toPx() }
    val heightDp = with(density) { (widthPx * ratio.h / ratio.w).toDp() }
    val depth = width * GeistBookTokens.DEPTH_RATIO
    val backRevealX = depth * GeistBookTokens.BACK_REVEAL_RATIO
    val backRevealXpx = with(density) { backRevealX.toPx() }
    val pagesVisibleW = depth * GeistBookTokens.PAGES_VISIBLE_RATIO

    val bookShape = rememberBookShape(rtl)

    var pressed by remember { mutableStateOf(false) }
    val spec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
    val pressProgress by animateFloatAsState(if (pressed) 1f else 0f, spec, label = "press")
    // Open amount in 0..1. Driven externally (e.g. a coverflow) when [openProgress]
    // is supplied, otherwise by the press interaction. Read inside graphicsLayer
    // blocks so an external driver animates without triggering recomposition.
    val progress: () -> Float = openProgress ?: { pressProgress }

    // LTR tilt opens the right edge away (negative rotY, pivot left).
    // RTL mirrors: positive rotY, pivot right.
    val targetRotY = if (rtl) -GeistBookTokens.PRESS_TILT_DEGREES else GeistBookTokens.PRESS_TILT_DEGREES

    // Container layout:
    //   LTR: [front][back reveal space]  — front at x=0, back slides rightward.
    //   RTL: [back reveal space][front]  — front at x=backRevealX, back slides leftward.
    val frontOffsetX = if (rtl) backRevealX else 0.dp
    val pagesOffsetX = if (rtl) frontOffsetX - pagesVisibleW else width - 1.dp

    Box(
        modifier = modifier
            .width(width + backRevealX)
            .height(heightDp)
            .then(
                if (openProgress == null) {
                    Modifier.pointerInput(onClick) {
                        awaitEachGesture {
                            awaitFirstDown()
                            pressed = true
                            val up = waitForUpOrCancellation()
                            pressed = false
                            if (up != null) onClick?.invoke()
                        }
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        BackCoverLayer(
            shape = bookShape,
            color = color.darken(GeistBookTokens.BACK_DARKEN_FRACTION),
            width = width,
            heightDp = heightDp,
            rtl = rtl,
            backRevealXpx = backRevealXpx,
            progress = progress,
        )

        PagesSlabLayer(
            width = pagesVisibleW + 1.dp,
            heightDp = heightDp,
            offsetX = pagesOffsetX,
            rtl = rtl,
            progress = progress,
        )

        FrontCoverLayer(
            title = title,
            color = color,
            textColor = textColor,
            variant = variant,
            textured = textured,
            width = width,
            heightDp = heightDp,
            bodyBackground = bodyBackground,
            shape = bookShape,
            offsetX = frontOffsetX,
            targetRotY = targetRotY,
            progress = progress,
            rtl = rtl,
            illustration = illustration,
        )
    }
}

// ---------- Back cover ----------

@Composable
private fun BackCoverLayer(
    shape: CornerBasedShape,
    color: Color,
    width: Dp,
    heightDp: Dp,
    rtl: Boolean,
    backRevealXpx: Float,
    progress: () -> Float,
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(heightDp)
            .graphicsLayer {
                // LTR: back starts flush under the front (x=0) and slides right.
                // RTL: back starts at the reveal edge and slides left.
                val base = if (rtl) backRevealXpx else 0f
                val dir = if (rtl) -1f else 1f
                translationX = base + dir * backRevealXpx * progress()
            }
            .clip(shape)
            .background(color),
    )
}

// ---------- Pages slab (trapezium) ----------

@Composable
private fun PagesSlabLayer(
    width: Dp,
    heightDp: Dp,
    offsetX: Dp,
    rtl: Boolean,
    progress: () -> Float,
) {
    Canvas(
        modifier = Modifier
            .offset(x = offsetX)
            .width(width)
            .height(heightDp)
            .graphicsLayer {
                val p = progress()
                alpha = p
                scaleX = p.coerceAtLeast(0.001f)
                // Grow from the side that touches the front cover.
                transformOrigin = TransformOrigin(if (rtl) 1f else 0f, 0.5f)
            },
    ) {
        val w = size.width
        val h = size.height
        val frontSideInset = 0f
        val backSideInset = frontSideInset + h * GeistBookTokens.PAGES_TAPER_RATIO

        val path = Path().apply {
            if (rtl) {
                // Right side tall (meets front), left side tapered (meets back).
                moveTo(w, frontSideInset)
                lineTo(0f, backSideInset)
                lineTo(0f, h - backSideInset)
                lineTo(w, h - frontSideInset)
            } else {
                moveTo(0f, frontSideInset)
                lineTo(w, backSideInset)
                lineTo(w, h - backSideInset)
                lineTo(0f, h - frontSideInset)
            }
            close()
        }

        drawPath(path = path, brush = pagesBrush(w, rtl))
    }
}

/**
 * Svelte `.bg-book-pages`: solid whitish base + gray overlay fading at 70%.
 * Horizontal gray→white gradient approximates the depth shading. Reversed for RTL.
 */
private fun pagesBrush(widthPx: Float, rtl: Boolean): Brush = Brush.horizontalGradient(
    colorStops = if (rtl) {
        arrayOf(
            0f to Color(0xFFFFFFFF), // near back (far from viewer)
            0.3f to Color(0xFFF6F6F6),
            1f to Color(0xFFDEDEDE), // near front (close to viewer)
        )
    } else {
        arrayOf(
            0f to Color(0xFFDEDEDE), // near front (close to viewer)
            0.7f to Color(0xFFF6F6F6),
            1f to Color(0xFFFFFFFF), // near back (far from viewer)
        )
    },
    startX = 0f,
    endX = widthPx,
)

// ---------- Front cover ----------

@Composable
private fun FrontCoverLayer(
    title: String,
    color: Color,
    textColor: Color,
    variant: GeistBookVariant,
    textured: Boolean,
    width: Dp,
    heightDp: Dp,
    bodyBackground: Color,
    shape: CornerBasedShape,
    offsetX: Dp,
    targetRotY: Float,
    progress: () -> Float,
    rtl: Boolean,
    illustration: (@Composable () -> Unit)?,
) {
    val density = LocalDensity.current
    val originX = if (rtl) 1f else 0f

    Box(
        modifier = Modifier
            .offset(x = offsetX)
            .width(width)
            .height(heightDp)
            .graphicsLayer {
                val p = progress()
                cameraDistance = GeistBookTokens.CAMERA_DISTANCE_MUL * density.density
                rotationY = targetRotY * p
                val s = 1f + (GeistBookTokens.PRESS_SCALE - 1f) * p
                scaleX = s
                scaleY = s
                transformOrigin = TransformOrigin(originX, 0.5f)
            }
            .shadow(
                elevation = GeistBookTokens.ShadowElevation,
                shape = shape,
                clip = false,
                ambientColor = Color.Black,
                spotColor = Color.Black,
            )
            .clip(shape)
            .background(bodyBackground),
    ) {
        when (variant) {
            GeistBookVariant.Stripe -> StripeCoverContent(
                title = title,
                color = color,
                textColor = textColor,
                width = width,
                bodyBackground = bodyBackground,
                textured = textured,
                rtl = rtl,
                illustration = illustration,
            )

            GeistBookVariant.Simple -> SimpleCoverContent(
                title = title,
                color = color,
                textColor = textColor,
                width = width,
                textured = textured,
                rtl = rtl,
                illustration = illustration,
            )

            GeistBookVariant.Image -> ImageCoverContent(
                title = title,
                textColor = textColor,
                width = width,
                textured = textured,
                rtl = rtl,
                illustration = illustration,
            )
        }
        SpineShadowLayer(
            rtl = rtl,
            modifier = Modifier
                .fillMaxHeight()
                .width(width * GeistBookTokens.SPINE_WIDTH_RATIO)
                .align(if (rtl) Alignment.CenterEnd else Alignment.CenterStart),
        )
    }
}

@Composable
private fun StripeCoverContent(
    title: String,
    color: Color,
    textColor: Color,
    width: Dp,
    bodyBackground: Color,
    textured: Boolean,
    rtl: Boolean,
    illustration: (@Composable () -> Unit)?,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .background(color),
        ) {
            if (illustration != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    illustration()
                }
            }
            if (textured) TexturedOverlay()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(bodyBackground),
        ) {
            BookBodyContent(
                title = title,
                textColor = textColor,
                width = width,
                showVercelLogo = true,
                illustration = null,
                simpleVariant = false,
                rtl = rtl,
            )
            if (textured) TexturedOverlay()
        }
    }
}

@Composable
private fun SimpleCoverContent(
    title: String,
    color: Color,
    textColor: Color,
    width: Dp,
    textured: Boolean,
    rtl: Boolean,
    illustration: (@Composable () -> Unit)?,
) {
    Box(modifier = Modifier.fillMaxSize().background(color)) {
        BookBodyContent(
            title = title,
            textColor = textColor,
            width = width,
            showVercelLogo = false,
            illustration = illustration,
            simpleVariant = true,
            rtl = rtl,
        )
        if (textured) TexturedOverlay()
    }
}

@Composable
private fun ImageCoverContent(
    title: String,
    textColor: Color,
    width: Dp,
    textured: Boolean,
    rtl: Boolean,
    illustration: (@Composable () -> Unit)?,
) {
    val padAll = width * GeistBookTokens.BODY_PADDING_RATIO
    val padSpine = width * GeistBookTokens.BODY_PADDING_SPINE_RATIO
    val titleSize = (width.value * GeistBookTokens.TITLE_SIZE_RATIO).sp
    val titleLineHeight = titleSize * GeistBookTokens.TITLE_LINE_HEIGHT_MUL
    val titleLetterSpacing = (GeistBookTokens.TITLE_LETTER_SPACING_MUL * titleSize.value).sp

    Box(modifier = Modifier.fillMaxSize()) {
        // Full-bleed illustration.
        if (illustration != null) {
            Box(Modifier.fillMaxSize()) { illustration() }
        }
        // Dark gradient scrim at the bottom so the title stays legible over
        // any image, regardless of brightness.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                    ),
                ),
        )
        // Title overlaid at the bottom.
        Text(
            text = title,
            style = TextStyle(
                color = textColor,
                fontSize = titleSize,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                lineHeight = titleLineHeight,
                letterSpacing = titleLetterSpacing,
                textAlign = if (rtl) TextAlign.Right else TextAlign.Left,
            ),
            modifier = Modifier
                .align(if (rtl) Alignment.BottomEnd else Alignment.BottomStart)
                .padding(
                    start = if (rtl) padAll else padSpine,
                    end = if (rtl) padSpine else padAll,
                    bottom = padAll,
                    top = padAll,
                ),
        )
        if (textured) TexturedOverlay()
    }
}

@Composable
private fun BookBodyContent(
    title: String,
    textColor: Color,
    width: Dp,
    showVercelLogo: Boolean,
    illustration: (@Composable () -> Unit)?,
    simpleVariant: Boolean,
    rtl: Boolean,
) {
    val padAll = width * GeistBookTokens.BODY_PADDING_RATIO
    val padSpine = width * GeistBookTokens.BODY_PADDING_SPINE_RATIO
    val titleSize = (width.value * GeistBookTokens.TITLE_SIZE_RATIO).sp
    val titleLineHeight = titleSize * GeistBookTokens.TITLE_LINE_HEIGHT_MUL
    val titleLetterSpacing = (GeistBookTokens.TITLE_LETTER_SPACING_MUL * titleSize.value).sp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = if (rtl) padAll else padSpine,
                top = padAll,
                end = if (rtl) padSpine else padAll,
                bottom = padAll,
            ),
        verticalArrangement = if (!simpleVariant) {
            Arrangement.SpaceBetween
        } else {
            Arrangement.spacedBy(width * 0.05f)
        },
        horizontalAlignment = if (rtl) Alignment.End else Alignment.Start,
    ) {
        Text(
            text = title,
            style = TextStyle(
                color = textColor,
                fontSize = titleSize,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                lineHeight = titleLineHeight,
                letterSpacing = titleLetterSpacing,
                textAlign = if (rtl) TextAlign.Right else TextAlign.Left,
            ),
        )
        Box(contentAlignment = if (rtl) Alignment.BottomEnd else Alignment.BottomStart) {
            when {
                showVercelLogo -> VercelLogo(
                    size = (width.value * GeistBookTokens.VERCEL_LOGO_RATIO).dp,
                    color = textColor,
                )

                illustration != null -> illustration()
                simpleVariant -> SimpleIllustration(
                    width = (width.value * GeistBookTokens.SIMPLE_ILLUSTRATION_RATIO).dp,
                )
            }
        }
    }
}

// ---------- Spine shadow ----------

@Composable
private fun SpineShadowLayer(
    rtl: Boolean,
    modifier: Modifier = Modifier,
) {
    // Svelte `.bg-book-bind` uses `mix-blend-overlay`. Compose can't stack
    // overlay-blend between layout children, so we approximate with a
    // Multiply-blended horizontal gradient via drawWithContent — darkest at
    // the outer spine edge, transparent at the inner edge. Stops are mirrored
    // for RTL so the gradient direction flips with the spine position.
    Box(
        modifier = modifier.drawWithContent {
            drawContent()
            val stops = arrayOf(
                0.00f to Color.Black.copy(alpha = 0.30f),
                0.15f to Color.Black.copy(alpha = 0.22f),
                0.35f to Color.Black.copy(alpha = 0.08f),
                0.55f to Color.Black.copy(alpha = 0.05f),
                0.76f to Color.Black.copy(alpha = 0.35f),
                0.80f to Color.Black.copy(alpha = 0.08f),
                1.00f to Color.Transparent,
            )
            val brush = Brush.horizontalGradient(
                colorStops = if (rtl) {
                    stops.map { (f, c) -> (1f - f) to c }
                        .sortedBy { it.first }
                        .toTypedArray()
                } else {
                    stops
                },
            )
            drawRect(
                brush = brush,
                topLeft = Offset.Zero,
                size = Size(size.width, size.height),
                blendMode = BlendMode.Multiply,
            )
        },
    )
}

// ---------- Shape & color helpers ----------

@Composable
private fun rememberBookShape(rtl: Boolean): CornerBasedShape {
    // Larger corner radius on the spine side, smaller on the outer edge.
    return remember(rtl) {
        if (rtl) {
            RoundedCornerShape(
                topStart = GeistBookTokens.OuterCorner,
                topEnd = GeistBookTokens.SpineCorner,
                bottomEnd = GeistBookTokens.SpineCorner,
                bottomStart = GeistBookTokens.OuterCorner,
            )
        } else {
            RoundedCornerShape(
                topStart = GeistBookTokens.SpineCorner,
                topEnd = GeistBookTokens.OuterCorner,
                bottomEnd = GeistBookTokens.OuterCorner,
                bottomStart = GeistBookTokens.SpineCorner,
            )
        }
    }
}

/** Darken a color by blending with black. 0f → same, 1f → pure black. */
private fun Color.darken(fraction: Float): Color = Color(
    red = red * (1f - fraction),
    green = green * (1f - fraction),
    blue = blue * (1f - fraction),
    alpha = alpha,
)

// ---------- Illustrations ----------

/** Vercel logo — solid equilateral triangle pointing up. */
@Composable
fun VercelLogo(size: Dp, color: Color = Color(0xFF000000)) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w / 2f, 0f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(path = path, color = color, style = Fill)
    }
}

/**
 * SimpleIllustration — port of geist's simple-illustration.svg (viewBox 36×56).
 * Three interlocking curved segments: teal top, blue middle lens, red bottom.
 */
@Composable
fun SimpleIllustration(
    width: Dp,
    blue: Color = Color(0xFF0070F3),
    teal: Color = Color(0xFF45DEC4),
    red: Color = Color(0xFFE5484D),
) {
    val height = width * (56f / 36f)
    Canvas(modifier = Modifier.size(width = width, height = height)) {
        val sx = this.size.width / 36f
        val sy = this.size.height / 56f
        fun Path.m(x: Float, y: Float) = moveTo(x * sx, y * sy)
        fun Path.c(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) =
            cubicTo(x1 * sx, y1 * sy, x2 * sx, y2 * sy, x3 * sx, y3 * sy)

        val tealPath = Path().apply {
            m(32.9691f, 28.0012f)
            c(34.8835f, 25.1411f, 36f, 21.7017f, 36f, 18.0015f)
            c(36f, 8.06034f, 27.9411f, 0.00146484f, 18f, 0.00146484f)
            c(8.05887f, 0.00146484f, 0f, 8.06034f, 0f, 18.0015f)
            c(0f, 21.7017f, 1.11648f, 25.1411f, 3.03094f, 28.0012f)
            c(6.25996f, 23.1771f, 11.7591f, 20.001f, 18f, 20.001f)
            c(24.2409f, 20.001f, 29.74f, 23.1771f, 32.9691f, 28.0012f)
            close()
        }
        val bluePath = Path().apply {
            m(3.03113f, 28.0005f)
            c(6.26017f, 23.1765f, 11.7592f, 20.0005f, 18f, 20.0005f)
            c(24.2409f, 20.0005f, 29.7399f, 23.1765f, 32.9689f, 28.0005f)
            c(29.7399f, 32.8244f, 24.2409f, 36.0005f, 18f, 36.0005f)
            c(11.7592f, 36.0005f, 6.26017f, 32.8244f, 3.03113f, 28.0005f)
            close()
        }
        val redPath = Path().apply {
            m(32.9692f, 28.0005f)
            c(29.7402f, 32.8247f, 24.241f, 36.001f, 18f, 36.001f)
            c(11.759f, 36.001f, 6.25977f, 32.8247f, 3.03077f, 28.0005f)
            c(1.11642f, 30.8606f, 0f, 34.2999f, 0f, 38f)
            c(0f, 47.9411f, 8.05887f, 56f, 18f, 56f)
            c(27.9411f, 56f, 36f, 47.9411f, 36f, 38f)
            c(36f, 34.2999f, 34.8836f, 30.8606f, 32.9692f, 28.0005f)
            close()
        }

        drawPath(path = tealPath, color = teal, style = Fill)
        drawPath(path = bluePath, color = blue, style = Fill)
        drawPath(path = redPath, color = red, style = Fill)
    }
}

@Composable
private fun TexturedOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.04f),
                        Color.Black.copy(alpha = 0.05f),
                    ),
                ),
            ),
    )
}
