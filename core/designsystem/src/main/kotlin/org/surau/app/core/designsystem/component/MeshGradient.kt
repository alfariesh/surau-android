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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.VertexMode
import androidx.compose.ui.graphics.Vertices
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import org.surau.app.core.designsystem.theme.LocalGradientColors
import org.surau.app.core.designsystem.theme.LocalMeshGradientColors
import org.surau.app.core.designsystem.theme.LocalMeshGradientEnabled
import org.surau.app.core.designsystem.theme.MeshGradientColors
import org.surau.app.core.designsystem.theme.SurauTheme
import kotlin.math.max
import kotlin.math.min

/**
 * Catmull-Rom interpolation of a scalar (tension 0.5), the smooth spline that gives a mesh gradient
 * its flowing, organic look — neighbouring control values curve into each other instead of meeting
 * in a straight line. Ported from the Apache-2.0 ComposeMeshGradient library
 * (io.github.om252345.composemeshgradient), whose author notes the CPU path matches its GPU shader.
 */
private fun catmullRom(p0: Float, p1: Float, p2: Float, p3: Float, t: Float): Float {
    val tn = 0.5f
    val t2 = t * t
    val t3 = t2 * t
    return ((-tn * p0 + (2f - tn) * p1 + (tn - 2f) * p2 + tn * p3) * t3) +
        (((2f * tn) * p0 + (tn - 3f) * p1 + (3f - 2f * tn) * p2 - tn * p3) * t2) +
        (((-tn) * p0 + tn * p2) * t) + p1
}

private fun catmullRomColor(c0: Color, c1: Color, c2: Color, c3: Color, t: Float): Color = Color(
    red = catmullRom(c0.red, c1.red, c2.red, c3.red, t).coerceIn(0f, 1f),
    green = catmullRom(c0.green, c1.green, c2.green, c3.green, t).coerceIn(0f, 1f),
    blue = catmullRom(c0.blue, c1.blue, c2.blue, c3.blue, t).coerceIn(0f, 1f),
    alpha = catmullRom(c0.alpha, c1.alpha, c2.alpha, c3.alpha, t).coerceIn(0f, 1f),
)

/**
 * Builds Compose [Vertices] for a Catmull-Rom mesh gradient. A [gridWidth]×[gridHeight] grid of
 * control [points] (normalized 0..1, row-major) and per-point [colors] is subdivided [subdivisions]
 * times per cell and smoothly splined in both axes — so warping the interior points warps the colour
 * flow. Two passes (X then Y) mirror the reference library's `generateMeshVerticesAndColors`.
 */
private fun buildCatmullRomVertices(
    gridWidth: Int,
    gridHeight: Int,
    points: List<Offset>,
    colors: List<Color>,
    subdivisions: Int,
    width: Float,
    height: Float,
): Vertices {
    val totalX = (gridWidth - 1) * subdivisions + 1
    val totalY = (gridHeight - 1) * subdivisions + 1

    fun pos(x: Int, y: Int) = points[y * gridWidth + x]
    fun col(x: Int, y: Int) = colors[y * gridWidth + x]

    // Pass 1 — spline along X for every control row.
    val rowPos = Array(gridHeight) { Array(totalX) { Offset.Zero } }
    val rowCol = Array(gridHeight) { Array(totalX) { Color.Transparent } }
    for (gy in 0 until gridHeight) {
        for (gx in 0 until gridWidth - 1) {
            val p0 = pos(max(gx - 1, 0), gy)
            val p1 = pos(gx, gy)
            val p2 = pos(gx + 1, gy)
            val p3 = pos(min(gx + 2, gridWidth - 1), gy)
            val c0 = col(max(gx - 1, 0), gy)
            val c1 = col(gx, gy)
            val c2 = col(gx + 1, gy)
            val c3 = col(min(gx + 2, gridWidth - 1), gy)
            for (i in 0 until subdivisions) {
                val t = i / subdivisions.toFloat()
                val ix = gx * subdivisions + i
                rowPos[gy][ix] =
                    Offset(catmullRom(p0.x, p1.x, p2.x, p3.x, t), catmullRom(p0.y, p1.y, p2.y, p3.y, t))
                rowCol[gy][ix] = catmullRomColor(c0, c1, c2, c3, t)
            }
        }
        rowPos[gy][(gridWidth - 1) * subdivisions] = pos(gridWidth - 1, gy)
        rowCol[gy][(gridWidth - 1) * subdivisions] = col(gridWidth - 1, gy)
    }

    // Pass 2 — spline along Y for every interpolated column.
    val positions = MutableList(totalX * totalY) { Offset.Zero }
    val vertexColors = MutableList(totalX * totalY) { Color.Transparent }
    for (ix in 0 until totalX) {
        for (gy in 0 until gridHeight - 1) {
            val p0 = rowPos[max(gy - 1, 0)][ix]
            val p1 = rowPos[gy][ix]
            val p2 = rowPos[gy + 1][ix]
            val p3 = rowPos[min(gy + 2, gridHeight - 1)][ix]
            val c0 = rowCol[max(gy - 1, 0)][ix]
            val c1 = rowCol[gy][ix]
            val c2 = rowCol[gy + 1][ix]
            val c3 = rowCol[min(gy + 2, gridHeight - 1)][ix]
            for (i in 0 until subdivisions) {
                val t = i / subdivisions.toFloat()
                val iy = gy * subdivisions + i
                val idx = iy * totalX + ix
                positions[idx] = Offset(
                    catmullRom(p0.x, p1.x, p2.x, p3.x, t) * width,
                    catmullRom(p0.y, p1.y, p2.y, p3.y, t) * height,
                )
                vertexColors[idx] = catmullRomColor(c0, c1, c2, c3, t)
            }
        }
        val lastY = (gridHeight - 1) * subdivisions
        val idx = lastY * totalX + ix
        positions[idx] = Offset(rowPos[gridHeight - 1][ix].x * width, rowPos[gridHeight - 1][ix].y * height)
        vertexColors[idx] = rowCol[gridHeight - 1][ix]
    }

    val indices = ArrayList<Int>((totalX - 1) * (totalY - 1) * 6)
    for (y in 0 until totalY - 1) {
        for (x in 0 until totalX - 1) {
            val i0 = y * totalX + x
            val i1 = i0 + 1
            val i2 = (y + 1) * totalX + x
            val i3 = i2 + 1
            indices.add(i0)
            indices.add(i2)
            indices.add(i3)
            indices.add(i0)
            indices.add(i3)
            indices.add(i1)
        }
    }
    return Vertices(VertexMode.Triangles, positions, positions, vertexColors, indices)
}

/**
 * A smooth Catmull-Rom mesh gradient filling its bounds, drawn on the Compose canvas (no GL surface,
 * so it clips, rounds and z-orders like any other composable). [points] are normalized 0..1 control
 * points (row-major, size `gridWidth*gridHeight`); warping the interior points bends the colour flow.
 * The mesh is cached in [drawWithCache] and only rebuilt when the size, grid or colors change.
 */
@Composable
fun SurauMeshGradient(
    gridWidth: Int,
    gridHeight: Int,
    points: List<Offset>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    subdivisions: Int = 14,
    content: @Composable () -> Unit = {},
) {
    Surface(color = Color.Transparent, modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val paint = Paint()
                    val vertices = buildCatmullRomVertices(
                        gridWidth,
                        gridHeight,
                        points,
                        colors,
                        subdivisions,
                        size.width,
                        size.height,
                    )
                    onDrawBehind {
                        drawIntoCanvas { canvas -> canvas.drawVertices(vertices, BlendMode.SrcOver, paint) }
                    }
                },
        ) {
            content()
        }
    }
}

/**
 * A living mesh gradient: the same Catmull-Rom mesh as [SurauMeshGradient], but the *interior*
 * control points drift continuously via simplex noise (borders stay pinned) for a slow, calming
 * "aurora" motion — the animation pattern from the reference library's SimplexNoise example. The
 * point positions are recomputed in the draw phase from a single per-frame clock, so the motion
 * never triggers recomposition. Pass [animated] = false (reduced motion / battery saver) to freeze
 * it to the base grid.
 */
@Composable
fun SurauAnimatedMeshGradient(
    gridWidth: Int,
    gridHeight: Int,
    basePoints: List<Offset>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    subdivisions: Int = 6,
    animationSpeed: Float = 0.2f,
    noiseIntensity: Float = 0.14f,
    animated: Boolean = true,
    content: @Composable () -> Unit = {},
) {
    val paint = remember { Paint() }
    var elapsedSeconds by remember { mutableFloatStateOf(0f) }
    if (animated) {
        LaunchedEffect(Unit) {
            var startNanos = 0L
            while (true) {
                withFrameNanos { now ->
                    if (startNanos == 0L) startNanos = now
                    elapsedSeconds = (now - startNanos) / 1_000_000_000f
                }
            }
        }
    }
    Surface(color = Color.Transparent, modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val time = if (animated) elapsedSeconds else 0f
                    val pts = animatedMeshPoints(
                        basePoints,
                        gridWidth,
                        gridHeight,
                        time,
                        animationSpeed,
                        noiseIntensity,
                    )
                    val vertices = buildCatmullRomVertices(
                        gridWidth,
                        gridHeight,
                        pts,
                        colors,
                        subdivisions,
                        size.width,
                        size.height,
                    )
                    drawIntoCanvas { canvas -> canvas.drawVertices(vertices, BlendMode.SrcOver, paint) }
                },
        ) {
            content()
        }
    }
}

/** Drifts the interior control points by simplex noise around their base grid; borders stay fixed. */
private fun animatedMeshPoints(
    base: List<Offset>,
    gridWidth: Int,
    gridHeight: Int,
    time: Float,
    speed: Float,
    intensity: Float,
): List<Offset> {
    if (time == 0f) return base
    return base.mapIndexed { i, bp ->
        val col = i % gridWidth
        val row = i / gridWidth
        val isBorder = row == 0 || row == gridHeight - 1 || col == 0 || col == gridWidth - 1
        if (isBorder) {
            bp
        } else {
            val nx = SurauSimplexNoise.noise(bp.x * 1.5f, time * speed + i) * intensity
            val ny = SurauSimplexNoise.noise(bp.y * 1.5f, time * speed + i + 100f) * intensity
            Offset(bp.x + nx, bp.y + ny)
        }
    }
}

private fun Color.orTransparent(): Color = if (this == Color.Unspecified) Color.Transparent else this

/**
 * A decorative mesh-gradient background for chrome "moments" (home header, expanded player, splash,
 * khatam celebration). Expands the four [MeshGradientColors] corners (already theme-derived and
 * alpha-capped) into a smooth, slightly off-centre 3×3 Catmull-Rom mesh. Cached and static.
 *
 * Never place this behind Arabic reading text in the *normal* reader — that stays on the flat
 * [SurauBackground]. (The Flow player is a deliberate exception; its ink stays high-contrast.)
 */
@Composable
fun SurauMeshGradientBackground(
    modifier: Modifier = Modifier,
    colors: MeshGradientColors = LocalMeshGradientColors.current,
    content: @Composable () -> Unit,
) {
    val tl = colors.topStart.orTransparent()
    val tr = colors.topEnd.orTransparent()
    val bl = colors.bottomStart.orTransparent()
    val br = colors.bottomEnd.orTransparent()
    val center = lerp(lerp(tl, tr, 0.5f), lerp(bl, br, 0.5f), 0.5f)
    val gridColors = listOf(
        tl, lerp(tl, tr, 0.5f), tr,
        lerp(tl, bl, 0.5f), center, lerp(tr, br, 0.5f),
        bl, lerp(bl, br, 0.5f), br,
    )
    SurauMeshGradient(
        gridWidth = 3,
        gridHeight = 3,
        points = ChromeMeshPoints,
        colors = gridColors,
        modifier = modifier,
        subdivisions = 12,
        content = content,
    )
}

/** A 3×3 grid with the centre nudged up-and-right so the chrome wash isn't a symmetric four-corner fade. */
private val ChromeMeshPoints = listOf(
    Offset(0f, 0f), Offset(0.5f, 0f), Offset(1f, 0f),
    Offset(0f, 0.5f), Offset(0.58f, 0.44f), Offset(1f, 0.5f),
    Offset(0f, 1f), Offset(0.5f, 1f), Offset(1f, 1f),
)

/**
 * The standard chrome background for dashboard surfaces. Renders the decorative mesh when the user
 * has enabled it (and runtime gates allow it — see [LocalMeshGradientEnabled]); otherwise falls back
 * to the subtle linear [SurauGradientBackground]. Either way the app's page color shows through, and
 * the reader is unaffected since it never uses this wrapper.
 */
@Composable
fun SurauChromeBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (LocalMeshGradientEnabled.current) {
        SurauMeshGradientBackground(modifier = modifier, content = content)
    } else {
        SurauGradientBackground(
            modifier = modifier,
            gradientColors = LocalGradientColors.current.copy(container = Color.Unspecified),
            content = content,
        )
    }
}

@ThemePreviews
@Composable
fun MeshGradientBackgroundPreview() {
    SurauTheme {
        SurauMeshGradientBackground(
            modifier = Modifier.size(120.dp),
            colors = MeshGradientColors(
                topStart = Color(0xFF58DBA4),
                topEnd = Color(0xFFF2BF42),
                bottomStart = Color(0xFF4FD8EB),
                bottomEnd = Color(0xFF6A994E),
            ),
            content = {},
        )
    }
}
