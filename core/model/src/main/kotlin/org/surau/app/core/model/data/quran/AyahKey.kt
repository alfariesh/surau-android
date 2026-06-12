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

package org.surau.app.core.model.data.quran

/**
 * A Quran ayah identifier in the backend's `"surah:ayah"` format, e.g. `"73:4"`.
 */
@JvmInline
value class AyahKey(val value: String) {

    val surahId: Int
        get() = value.substringBefore(':').toInt()

    val ayahNumber: Int
        get() = value.substringAfter(':').toInt()

    init {
        require(REGEX.matches(value)) { "Invalid ayah key: $value" }
    }

    override fun toString(): String = value

    companion object {
        private val REGEX = Regex("""\d{1,3}:\d{1,3}""")

        fun of(surahId: Int, ayahNumber: Int): AyahKey = AyahKey("$surahId:$ayahNumber")
    }
}
