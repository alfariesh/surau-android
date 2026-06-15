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

package org.surau.app.core.datastore.crypto

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import org.surau.app.core.datastore.crypto.AuthCrypto.Companion.CIPHER_PREFIX
import javax.inject.Inject

/**
 * [AuthCrypto] backed by an AES-256-GCM AEAD whose keyset is wrapped by an Android Keystore master
 * key. The wrapped keyset lives in a dedicated SharedPreferences file (excluded from backup); the
 * master key never leaves the Keystore (TEE/StrongBox where available).
 *
 * Initialisation is lazy and failure-tolerant: if the Keystore is unavailable the AEAD stays null
 * and this degrades to plaintext storage rather than blocking sign-in. Decryption failures (e.g. a
 * key invalidated after a lock-screen change) surface as null so callers re-authenticate cleanly.
 */
internal class TinkAuthCrypto @Inject constructor(
    @ApplicationContext private val context: Context,
) : AuthCrypto {

    private val aead: Aead? by lazy { createAead() }

    override fun encrypt(plaintext: String): String {
        val cipher = aead ?: return plaintext
        return try {
            CIPHER_PREFIX + Base64.encodeToString(
                cipher.encrypt(plaintext.toByteArray(Charsets.UTF_8), ASSOCIATED_DATA),
                Base64.NO_WRAP,
            )
        } catch (_: Exception) {
            // Never block a sign-in on an encryption hiccup; fall back to plaintext-at-rest.
            plaintext
        }
    }

    override fun decrypt(stored: String): String? {
        if (!stored.startsWith(CIPHER_PREFIX)) return stored // legacy / fallback plaintext
        val cipher = aead ?: return null
        return try {
            val bytes = Base64.decode(stored.removePrefix(CIPHER_PREFIX), Base64.NO_WRAP)
            String(cipher.decrypt(bytes, ASSOCIATED_DATA), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    private fun createAead(): Aead? = try {
        AeadConfig.register()
        AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREF_FILE)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
            .getPrimitive(Aead::class.java)
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val KEYSET_NAME = "surau_auth_keyset"
        const val KEYSET_PREF_FILE = "surau_auth_keyset"
        const val MASTER_KEY_URI = "android-keystore://surau_auth_master_key"
        val ASSOCIATED_DATA = "surau.auth.token".toByteArray(Charsets.UTF_8)
    }
}
