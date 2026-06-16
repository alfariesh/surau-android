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
     * Flow reader can pick the surah-mode reciter.
     *
     * The existing rows are cleared as well: `recitations` is a weekly-refreshed cache, and rows
     * written before this column existed would carry `mode = NULL`. Since the cache still looks
     * fresh (within its TTL), the repository would skip the network refresh and never learn each
     * reciter's mode, leaving Flow unable to resolve the surah-mode reciter. Emptying the table
     * forces a refetch on next access, which backfills `mode` for every row.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE recitations ADD COLUMN mode TEXT")
            db.execSQL("DELETE FROM recitations")
        }
    }

    /**
     * v3 → v4: adds the `bookmarks` table for milestone 3 (saved ayat). Purely additive; existing
     * tables are untouched. The `CREATE TABLE`/`CREATE INDEX` statements mirror the Room-generated
     * v4 schema (see `schemas/.../4.json`) verbatim.
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `bookmarks` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`ayah_key` TEXT NOT NULL, " +
                    "`surah_id` INTEGER NOT NULL, " +
                    "`label` TEXT, " +
                    "`note` TEXT, " +
                    "`tags` TEXT NOT NULL, " +
                    "`created_at` INTEGER NOT NULL, " +
                    "`updated_at` INTEGER NOT NULL, " +
                    "`server_id` TEXT, " +
                    "`pending_sync` INTEGER NOT NULL, " +
                    "`pending_delete` INTEGER NOT NULL)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_bookmarks_ayah_key` " +
                    "ON `bookmarks` (`ayah_key`)",
            )
        }
    }

    /**
     * v4 → v5: milestone 6 offline search. Adds two nullable Arabic columns to `ayahs`
     * (`text_imlaei_simple`, `search_text`, only filled by the full-download path) and the
     * standalone `ayahs_fts` FTS4 table used for the offline-search fallback. Purely additive;
     * existing rows keep their values and the new columns default to NULL. The `CREATE VIRTUAL
     * TABLE` statement is copied verbatim from the Room-generated v5 schema (`schemas/.../5.json`)
     * so `runMigrationsAndValidate` accepts it.
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `ayahs` ADD COLUMN `text_imlaei_simple` TEXT")
            db.execSQL("ALTER TABLE `ayahs` ADD COLUMN `search_text` TEXT")
            db.execSQL(
                "CREATE VIRTUAL TABLE IF NOT EXISTS `ayahs_fts` USING FTS4(" +
                    "`ayah_key` TEXT NOT NULL, `source_id` TEXT NOT NULL, " +
                    "`arabic` TEXT NOT NULL, `translation` TEXT NOT NULL, " +
                    "notindexed=`ayah_key`, notindexed=`source_id`)",
            )
        }
    }
}
