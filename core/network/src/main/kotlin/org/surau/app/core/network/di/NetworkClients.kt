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

package org.surau.app.core.network.di

import javax.inject.Qualifier

/**
 * The unauthenticated client: HTTP cache enabled, no Bearer header. Used for public Quran
 * content and the auth endpoints themselves.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PublicClient

/**
 * The authenticated client: attaches Bearer tokens and refreshes them on 401. No HTTP cache —
 * personal data must never be served stale or persisted in the cache dir.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthClient
