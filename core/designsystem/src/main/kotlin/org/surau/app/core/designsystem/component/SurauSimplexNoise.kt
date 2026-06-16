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

import kotlin.math.floor
import kotlin.random.Random

/**
 * 2D simplex noise (Ken Perlin's algorithm) — smooth, continuous noise in [-1, 1] used to drift the
 * animated mesh's control points organically. Ported from the Apache-2.0 ComposeMeshGradient library
 * (io.github.om252345.composemeshgradient). The permutation is seeded so the motion is reproducible.
 */
internal object SurauSimplexNoise {
    private val grad3 = arrayOf(
        intArrayOf(1, 1, 0), intArrayOf(-1, 1, 0), intArrayOf(1, -1, 0), intArrayOf(-1, -1, 0),
        intArrayOf(1, 0, 1), intArrayOf(-1, 0, 1), intArrayOf(1, 0, -1), intArrayOf(-1, 0, -1),
        intArrayOf(0, 1, 1), intArrayOf(0, -1, 1), intArrayOf(0, 1, -1), intArrayOf(0, -1, -1),
    )

    private val perm = IntArray(512)
    private val permMod12 = IntArray(512)

    init {
        val p = (0..255).toMutableList().apply { shuffle(Random(2345)) }
        for (i in 0 until 512) {
            perm[i] = p[i and 255]
            permMod12[i] = perm[i] % 12
        }
    }

    private const val F2 = 0.3660254f // 0.5 * (sqrt(3) - 1)
    private const val G2 = 0.21132487f // (3 - sqrt(3)) / 6

    fun noise(xin: Float, yin: Float): Float {
        val s = (xin + yin) * F2
        val i = floor(xin + s).toInt()
        val j = floor(yin + s).toInt()
        val t = (i + j) * G2
        val x0 = xin - (i - t)
        val y0 = yin - (j - t)

        val (i1, j1) = if (x0 > y0) 1 to 0 else 0 to 1

        val x1 = x0 - i1 + G2
        val y1 = y0 - j1 + G2
        val x2 = x0 - 1f + 2f * G2
        val y2 = y0 - 1f + 2f * G2

        val ii = i and 255
        val jj = j and 255

        var n0 = 0f
        var n1 = 0f
        var n2 = 0f

        var t0 = 0.5f - x0 * x0 - y0 * y0
        if (t0 >= 0) {
            val gi0 = permMod12[ii + perm[jj]]
            t0 *= t0
            n0 = t0 * t0 * dot(grad3[gi0], x0, y0)
        }
        var t1 = 0.5f - x1 * x1 - y1 * y1
        if (t1 >= 0) {
            val gi1 = permMod12[ii + i1 + perm[jj + j1]]
            t1 *= t1
            n1 = t1 * t1 * dot(grad3[gi1], x1, y1)
        }
        var t2 = 0.5f - x2 * x2 - y2 * y2
        if (t2 >= 0) {
            val gi2 = permMod12[ii + 1 + perm[jj + 1]]
            t2 *= t2
            n2 = t2 * t2 * dot(grad3[gi2], x2, y2)
        }
        return 70f * (n0 + n1 + n2)
    }

    private fun dot(g: IntArray, x: Float, y: Float): Float = g[0] * x + g[1] * y
}
