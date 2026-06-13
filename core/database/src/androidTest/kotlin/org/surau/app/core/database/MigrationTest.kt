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
import kotlin.test.assertTrue

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
    fun migrate2To3_addsModeColumn_andKeepsExistingData() {
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

        // The new `mode` column exists and is NULL for the pre-migration row.
        db.query("SELECT mode FROM recitations WHERE id = 'mishari'").use { cursor ->
            cursor.moveToFirst()
            assertTrue(cursor.isNull(0))
        }
        // ...and is writable.
        db.execSQL("UPDATE recitations SET mode = 'surah' WHERE id = 'mishari'")
        db.query("SELECT mode FROM recitations WHERE id = 'mishari'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("surah", cursor.getString(0))
        }
        db.close()
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
