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

package org.surau.app.core.database.util

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Stores a [List] of [String] (e.g. bookmark tags) as a JSON-array string in a single column.
 * JSON keeps it robust against tags that contain commas or other delimiter characters.
 */
internal class StringListConverter {

    @TypeConverter
    fun fromJson(value: String?): List<String> =
        if (value.isNullOrEmpty()) emptyList() else json.decodeFromString(SERIALIZER, value)

    @TypeConverter
    fun toJson(list: List<String>): String = json.encodeToString(SERIALIZER, list)

    private companion object {
        val json = Json
        val SERIALIZER = ListSerializer(String.serializer())
    }
}
