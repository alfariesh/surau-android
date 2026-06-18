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

package org.surau.app.util

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Per-app language switching that works with a plain [androidx.activity.ComponentActivity] across
 * the whole minSdk 23 range.
 *
 * On API 33+ the framework's [LocaleManager] owns the per-app locale (surfaced in system Settings):
 * we set it directly so the system applies it and recreates the activity. We deliberately do NOT go
 * through `AppCompatDelegate` here — it only reliably applies locales to an `AppCompatActivity`, and
 * this is a plain Compose `ComponentActivity`. On API < 33 there is no framework support, so we
 * persist the choice ourselves and re-apply it in [wrap] (called from `MainActivity.attachBaseContext`).
 *
 * The empty string means "follow the system language".
 */
object AppLanguage {

    const val SYSTEM_TAG = ""

    val supportedTags = listOf(SYSTEM_TAG, "id", "en")

    private const val PREFS = "surau_locale"
    private const val KEY_TAG = "language_tag"

    /** The currently selected language tag, or [SYSTEM_TAG] when following the system. */
    fun current(context: Context): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)
                ?.applicationLocales
                ?.toLanguageTags()
                ?.substringBefore(',')
                .orEmpty()
        } else {
            prefs(context).getString(KEY_TAG, SYSTEM_TAG).orEmpty()
        }

    /** Applies and persists [tag]. The system recreates the activity (33+) or we recreate it (<33). */
    fun apply(context: Context, tag: String) {
        if (tag == current(context)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Framework per-app locale: the system stores it, applies it, and recreates the activity.
            context.getSystemService(LocaleManager::class.java)?.applicationLocales =
                if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        } else {
            // No framework support below 33: persist and re-apply via attachBaseContext on recreate.
            prefs(context).edit().putString(KEY_TAG, tag).apply()
            context.findActivity()?.recreate()
        }
    }

    /**
     * Wraps a base context with the persisted locale on API < 33. A no-op on 33+, where the
     * framework has already applied the per-app locale to the context.
     */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = prefs(base).getString(KEY_TAG, SYSTEM_TAG).orEmpty()
        if (tag.isEmpty()) return base
        val config = android.content.res.Configuration(base.resources.configuration)
        config.setLocale(Locale.forLanguageTag(tag))
        return base.createConfigurationContext(config)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
