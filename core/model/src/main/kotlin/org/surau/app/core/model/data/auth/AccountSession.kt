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

package org.surau.app.core.model.data.auth

import kotlinx.datetime.Instant

/**
 * One active session/device on the user's account. [isCurrent] marks the session this device is
 * signed in with — it cannot be revoked individually (use logout-all or delete-account instead).
 */
data class AccountSession(
    val id: String,
    val userAgent: String,
    val clientIp: String,
    val createdAt: Instant?,
    val lastUsedAt: Instant?,
    val expiresAt: Instant?,
    val isCurrent: Boolean,
)
