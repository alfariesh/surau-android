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

package org.surau.app.core.network.di

import android.content.Context
import android.os.Build
import androidx.tracing.trace
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.util.DebugLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.surau.app.core.network.BuildConfig
import org.surau.app.core.network.auth.AuthInterceptor
import org.surau.app.core.network.auth.TokenAuthenticator
import org.surau.app.core.network.retrofit.SurauAccountApi
import org.surau.app.core.network.retrofit.SurauAuthApi
import org.surau.app.core.network.retrofit.SurauMeApi
import org.surau.app.core.network.retrofit.SurauQuranApi
import org.surau.app.core.network.retrofit.SurauUserApi
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import javax.inject.Singleton

private const val HTTP_CACHE_SIZE_BYTES = 50L * 1024 * 1024

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {

    @Provides
    @Singleton
    fun providesNetworkJson(): Json = Json {
        ignoreUnknownKeys = true
        // Pinned explicitly (these match the kotlinx defaults) because the saved-items contract
        // depends on both: CreateSavedItem omits null fields (encodeDefaults off) so the server keeps
        // stored values, while PatchSavedItem emits explicit nulls (explicitNulls on) to clear one.
        encodeDefaults = false
        explicitNulls = true
    }

    private fun loggingInterceptor() = HttpLoggingInterceptor()
        .apply {
            if (BuildConfig.DEBUG) {
                setLevel(HttpLoggingInterceptor.Level.BODY)
            }
            redactHeader("Authorization")
        }

    /**
     * Sends a recognisable `User-Agent` (e.g. `Surau (Android 16; Google Pixel 8)`) instead of the
     * default `okhttp/x.y.z`, so the device shows up meaningfully in the account's session list and
     * the backend's "new sign-in" security emails.
     */
    private fun userAgentInterceptor(): Interceptor {
        val manufacturer = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercaseChar() }
        val model = Build.MODEL.orEmpty()
        val device = when {
            model.isBlank() && manufacturer.isBlank() -> "Android"
            model.startsWith(manufacturer, ignoreCase = true) -> model
            else -> "$manufacturer $model".trim()
        }
        val userAgent = "Surau (Android ${Build.VERSION.RELEASE}; $device)"
        return Interceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", userAgent)
                    .build(),
            )
        }
    }

    /**
     * Client for public endpoints. The disk cache honours the backend's ETag /
     * `Cache-Control: max-age=3600` headers on Quran content for free.
     */
    @Provides
    @Singleton
    @PublicClient
    fun publicOkHttpClient(
        @ApplicationContext context: Context,
    ): OkHttpClient = trace("SurauPublicOkHttpClient") {
        OkHttpClient.Builder()
            .cache(Cache(File(context.cacheDir, "http_cache"), HTTP_CACHE_SIZE_BYTES))
            .addInterceptor(userAgentInterceptor())
            .addInterceptor(loggingInterceptor())
            .build()
    }

    /**
     * Client for authenticated endpoints: Bearer header + single-flight token refresh on 401,
     * and no HTTP cache.
     */
    @Provides
    @Singleton
    @AuthClient
    fun authOkHttpClient(
        @PublicClient publicClient: OkHttpClient,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient = trace("SurauAuthOkHttpClient") {
        publicClient.newBuilder()
            .cache(null)
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .build()
    }

    private fun retrofit(json: Json, client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BACKEND_URL)
        .callFactory { client.newCall(it) }
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun providesAuthApi(
        json: Json,
        @PublicClient client: OkHttpClient,
    ): SurauAuthApi = retrofit(json, client).create(SurauAuthApi::class.java)

    @Provides
    @Singleton
    fun providesQuranApi(
        json: Json,
        @PublicClient client: OkHttpClient,
    ): SurauQuranApi = retrofit(json, client).create(SurauQuranApi::class.java)

    @Provides
    @Singleton
    fun providesUserApi(
        json: Json,
        @AuthClient client: OkHttpClient,
    ): SurauUserApi = retrofit(json, client).create(SurauUserApi::class.java)

    @Provides
    @Singleton
    fun providesAccountApi(
        json: Json,
        @AuthClient client: OkHttpClient,
    ): SurauAccountApi = retrofit(json, client).create(SurauAccountApi::class.java)

    @Provides
    @Singleton
    fun providesMeApi(
        json: Json,
        @AuthClient client: OkHttpClient,
    ): SurauMeApi = retrofit(json, client).create(SurauMeApi::class.java)

    /**
     * Since we're displaying SVGs in the app, Coil needs an ImageLoader which supports this
     * format. During Coil's initialization it will call `applicationContext.newImageLoader()` to
     * obtain an ImageLoader.
     *
     * @see <a href="https://github.com/coil-kt/coil/blob/main/coil-singleton/src/main/java/coil/Coil.kt">Coil</a>
     */
    @Provides
    @Singleton
    fun imageLoader(
        @PublicClient okHttpClient: dagger.Lazy<OkHttpClient>,
        @ApplicationContext application: Context,
    ): ImageLoader = trace("SurauImageLoader") {
        ImageLoader.Builder(application)
            .callFactory { okHttpClient.get() }
            .components { add(SvgDecoder.Factory()) }
            // Assume most content images are versioned urls
            // but some problematic images are fetching each time
            .respectCacheHeaders(false)
            .apply {
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()
    }
}
