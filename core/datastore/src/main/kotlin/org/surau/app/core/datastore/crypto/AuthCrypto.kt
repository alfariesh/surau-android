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

/**
 * Reversible obfuscation for the at-rest auth tokens. Implemented with Android Keystore-backed AEAD
 * (Tink) in production; a no-op identity in tests.
 *
 * Ciphertext is self-describing via a version prefix so a value written before encryption existed
 * (or on a device where Keystore was unavailable) reads back untouched — see [PlaintextAuthCrypto].
 */
interface AuthCrypto {
    /** Encrypts [plaintext] for storage. Returns plaintext unchanged if encryption is unavailable. */
    fun encrypt(plaintext: String): String

    /**
     * Decrypts a [stored] value. Returns it unchanged when it carries no ciphertext prefix (legacy
     * plaintext); returns null only when a prefixed value can't be decrypted (corrupt/invalidated
     * keyset), which callers treat as "no token".
     */
    fun decrypt(stored: String): String?

    companion object {
        /** Marks a value produced by [encrypt]; absence means legacy/fallback plaintext. */
        const val CIPHER_PREFIX = "v1:"
    }
}

/**
 * Identity [AuthCrypto] used as the default in tests and as the production fallback when Keystore
 * AEAD can't be initialised. Stores and returns plaintext verbatim.
 */
object PlaintextAuthCrypto : AuthCrypto {
    override fun encrypt(plaintext: String): String = plaintext
    override fun decrypt(stored: String): String? = stored
}
