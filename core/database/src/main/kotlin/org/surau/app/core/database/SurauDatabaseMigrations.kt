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

package org.surau.app.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal object SurauDatabaseMigrations {

    /**
     * v1 → v2: adds the `recitations` cache table for milestone 2 audio murottal. Purely additive;
     * existing tables are untouched. The `CREATE TABLE` statement mirrors the Room-generated v2
     * schema (see `schemas/.../2.json`).
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `recitations` (" +
                    "`id` TEXT NOT NULL, " +
                    "`display_name` TEXT NOT NULL, " +
                    "`reciter_name` TEXT NOT NULL, " +
                    "`style` TEXT, " +
                    "`is_default` INTEGER NOT NULL, " +
                    "`fetched_at` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))",
            )
        }
    }

    /**
     * v2 → v3: adds the nullable `mode` column ("ayah"/"surah") to `recitations` so the immersive
     * Flow reader can pick the surah-mode reciter. Purely additive.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE recitations ADD COLUMN mode TEXT")
        }
    }
}
