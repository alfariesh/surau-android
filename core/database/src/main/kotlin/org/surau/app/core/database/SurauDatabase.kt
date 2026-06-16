/*
 * Copyright 2022 The Android Open Source Project
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

package org.surau.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.surau.app.core.database.dao.AyahDao
import org.surau.app.core.database.dao.BookmarkDao
import org.surau.app.core.database.dao.JuzDao
import org.surau.app.core.database.dao.ReadingProgressDao
import org.surau.app.core.database.dao.RecitationDao
import org.surau.app.core.database.dao.SurahDao
import org.surau.app.core.database.dao.TranslationSourceDao
import org.surau.app.core.database.model.AyahEntity
import org.surau.app.core.database.model.AyahFetchMetadataEntity
import org.surau.app.core.database.model.AyahFtsEntity
import org.surau.app.core.database.model.BookmarkEntity
import org.surau.app.core.database.model.JuzEntity
import org.surau.app.core.database.model.ReadingProgressEntity
import org.surau.app.core.database.model.RecitationEntity
import org.surau.app.core.database.model.SurahEntity
import org.surau.app.core.database.model.TranslationEntity
import org.surau.app.core.database.model.TranslationSourceEntity
import org.surau.app.core.database.model.TransliterationEntity
import org.surau.app.core.database.util.InstantConverter
import org.surau.app.core.database.util.StringListConverter

@Database(
    entities = [
        SurahEntity::class,
        AyahEntity::class,
        TranslationEntity::class,
        TransliterationEntity::class,
        TranslationSourceEntity::class,
        RecitationEntity::class,
        JuzEntity::class,
        ReadingProgressEntity::class,
        AyahFetchMetadataEntity::class,
        BookmarkEntity::class,
        AyahFtsEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(
    InstantConverter::class,
    StringListConverter::class,
)
abstract class SurauDatabase : RoomDatabase() {
    abstract fun surahDao(): SurahDao
    abstract fun ayahDao(): AyahDao
    abstract fun translationSourceDao(): TranslationSourceDao
    abstract fun recitationDao(): RecitationDao
    abstract fun juzDao(): JuzDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun bookmarkDao(): BookmarkDao
}
