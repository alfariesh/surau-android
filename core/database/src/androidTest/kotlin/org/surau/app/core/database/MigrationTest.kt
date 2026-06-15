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

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SurauDatabase::class.java,
    )

    @Test
    fun migrate1To2_addsRecitationsTable_andKeepsExistingData() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO translation_sources " +
                    "(id, lang, name, translator, coverage_percent, is_default, fetched_at) " +
                    "VALUES ('kemenag-id-translation', 'id', 'Kemenag', NULL, NULL, 1, 0)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            2,
            true,
            SurauDatabaseMigrations.MIGRATION_1_2,
        )

        // The new recitations table exists and is empty.
        db.query("SELECT COUNT(*) FROM recitations").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        // Existing v1 data survived the migration.
        db.query("SELECT COUNT(*) FROM translation_sources").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrate2To3_addsModeColumn_andClearsStaleCache() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                "INSERT INTO recitations " +
                    "(id, display_name, reciter_name, style, is_default, fetched_at) " +
                    "VALUES ('mishari', 'Mishari', 'Mishari', 'murattal', 1, 0)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            SurauDatabaseMigrations.MIGRATION_2_3,
        )

        // The pre-`mode` cache row is cleared so the catalog refetches and backfills `mode`.
        db.query("SELECT COUNT(*) FROM recitations").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        // The new `mode` column exists and is writable.
        db.execSQL(
            "INSERT INTO recitations " +
                "(id, display_name, reciter_name, style, mode, is_default, fetched_at) " +
                "VALUES ('yasser', 'Yasser', 'Yasser', NULL, 'surah', 0, 1)",
        )
        db.query("SELECT mode FROM recitations WHERE id = 'yasser'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("surah", cursor.getString(0))
        }
        db.close()
    }

    @Test
    fun migrate3To4_addsBookmarksTable_andKeepsExistingData() {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(
                "INSERT INTO reading_progress (id, ayah_key, updated_at, pending_sync) " +
                    "VALUES (0, '2:255', 0, 0)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            4,
            true,
            SurauDatabaseMigrations.MIGRATION_3_4,
        )

        // The new bookmarks table exists and is empty.
        db.query("SELECT COUNT(*) FROM bookmarks").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        // Existing v3 data survived the migration.
        db.query("SELECT ayah_key FROM reading_progress WHERE id = 0").use { cursor ->
            cursor.moveToFirst()
            assertEquals("2:255", cursor.getString(0))
        }
        // The bookmarks schema is writable, including the JSON tags column.
        db.execSQL(
            "INSERT INTO bookmarks " +
                "(ayah_key, surah_id, label, note, tags, created_at, updated_at, " +
                "server_id, pending_sync, pending_delete) " +
                "VALUES ('73:4', 73, NULL, 'note', '[\"tafsir\"]', 0, 0, NULL, 1, 0)",
        )
        db.query("SELECT tags FROM bookmarks WHERE ayah_key = '73:4'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("[\"tafsir\"]", cursor.getString(0))
        }
        db.close()
    }

    @Test
    fun migrate4To5_addsImlaeiColumns_andFtsTable_keepingExistingData() {
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL(
                "INSERT INTO ayahs " +
                    "(surah_id, ayah_number, ayah_key, text_qpc_hafs, page_number, " +
                    "juz_number, hizb_number) " +
                    "VALUES (1, 1, '1:1', 'بِسْمِ', 1, 1, 1)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            5,
            true,
            SurauDatabaseMigrations.MIGRATION_4_5,
        )

        // Existing v4 ayah survived; the new columns exist and default to NULL.
        db.query("SELECT text_imlaei_simple, search_text FROM ayahs WHERE ayah_key = '1:1'")
            .use { cursor ->
                cursor.moveToFirst()
                assertEquals(true, cursor.isNull(0))
                assertEquals(true, cursor.isNull(1))
            }
        // The new imlaei/search columns are writable.
        db.execSQL(
            "UPDATE ayahs SET text_imlaei_simple = 'بسم', search_text = 'bsm' " +
                "WHERE ayah_key = '1:1'",
        )
        // The FTS table exists and MATCH works against the translation column.
        db.execSQL(
            "INSERT INTO ayahs_fts (ayah_key, source_id, arabic, translation) " +
                "VALUES ('1:1', 'kemenag-id-translation', 'bsm', 'Dengan nama Allah')",
        )
        db.query(
            "SELECT ayah_key FROM ayahs_fts WHERE ayahs_fts MATCH 'Allah'",
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals("1:1", cursor.getString(0))
        }
        db.close()
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
