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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.VertexMode
import androidx.compose.ui.graphics.Vertices
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import org.surau.app.core.designsystem.theme.LocalGradientColors
import org.surau.app.core.designsystem.theme.LocalMeshGradientColors
import org.surau.app.core.designsystem.theme.LocalMeshGradientEnabled
import org.surau.app.core.designsystem.theme.MeshGradientColors
import org.surau.app.core.designsystem.theme.SurauTheme

/** Subdivisions per axis. Higher = smoother diagonal blends at a small per-build cost. */
private const val MESH_RESOLUTION = 10

/**
 * Builds a four-corner bilinear color mesh as Compose [Vertices]. The geometry (a `res+1` square grid
 * triangulated into two triangles per cell) and the per-vertex colors are computed once; the colors
 * are interpolated bilinearly from the four [MeshGradientColors] corners so the diagonal stays smooth
 * even though `drawVertices` only interpolates linearly within each small triangle.
 *
 * This is the cleaned-up replacement for the old hand-rolled cubic mesh: no global mutable paint, no
 * `BlendMode.Dst` trickery, no scale-to-unit-square dance — just plain pixel-space positions.
 */
internal fun buildMeshVertices(
    width: Float,
    height: Float,
    colors: MeshGradientColors,
    resolution: Int = MESH_RESOLUTION,
): Vertices {
    val topStart = colors.topStart.orTransparent()
    val topEnd = colors.topEnd.orTransparent()
    val bottomStart = colors.bottomStart.orTransparent()
    val bottomEnd = colors.bottomEnd.orTransparent()

    val cols = resolution + 1
    val positions = ArrayList<Offset>(cols * cols)
    val vertexColors = ArrayList<Color>(cols * cols)
    for (yi in 0..resolution) {
        val v = yi / resolution.toFloat()
        val left = lerp(topStart, bottomStart, v)
        val right = lerp(topEnd, bottomEnd, v)
        for (xi in 0..resolution) {
            val u = xi / resolution.toFloat()
            positions.add(Offset(u * width, v * height))
            vertexColors.add(lerp(left, right, u))
        }
    }

    val indices = ArrayList<Int>(resolution * resolution * 6)
    for (yi in 0 until resolution) {
        for (xi in 0 until resolution) {
            val a = yi * cols + xi
            val b = a + 1
            val c = (yi + 1) * cols + xi
            val d = c + 1
            indices.add(a)
            indices.add(c)
            indices.add(d)
            indices.add(a)
            indices.add(d)
            indices.add(b)
        }
    }

    return Vertices(
        vertexMode = VertexMode.Triangles,
        positions = positions,
        textureCoordinates = positions,
        colors = vertexColors,
        indices = indices,
    )
}

/** Draws a four-corner mesh gradient filling the current [DrawScope]. Recomputes each call. */
internal fun DrawScope.drawMeshGradient(
    colors: MeshGradientColors,
    resolution: Int = MESH_RESOLUTION,
) {
    val vertices = buildMeshVertices(size.width, size.height, colors, resolution)
    drawIntoCanvas { canvas -> canvas.drawVertices(vertices, BlendMode.SrcOver, Paint()) }
}

private fun Color.orTransparent(): Color = if (this == Color.Unspecified) Color.Transparent else this

/**
 * A decorative mesh-gradient background for chrome "moments" (home header, expanded player, splash,
 * khatam celebration). The mesh is theme-driven (corners come from [LocalMeshGradientColors], already
 * alpha-capped) and **static**: the [Vertices] are cached in [drawWithCache] and only rebuilt when the
 * size or theme changes, so it is no more expensive per frame than the linear gradient.
 *
 * Never place this behind Arabic reading text — the reader stays on the flat [SurauBackground].
 */
@Composable
fun SurauMeshGradientBackground(
    modifier: Modifier = Modifier,
    colors: MeshGradientColors = LocalMeshGradientColors.current,
    content: @Composable () -> Unit,
) {
    Surface(color = Color.Transparent, modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val paint = Paint()
                    val vertices = buildMeshVertices(size.width, size.height, colors)
                    onDrawBehind {
                        drawIntoCanvas { canvas ->
                            canvas.drawVertices(vertices, BlendMode.SrcOver, paint)
                        }
                    }
                },
        ) {
            content()
        }
    }
}

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
                topStart = Color(0x3358DBA4),
                topEnd = Color(0x33F2BF42),
                bottomStart = Color(0x334FD8EB),
                bottomEnd = Color.Transparent,
            ),
            content = {},
        )
    }
}
