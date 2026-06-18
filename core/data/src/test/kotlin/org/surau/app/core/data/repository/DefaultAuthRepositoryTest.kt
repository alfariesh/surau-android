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

package org.surau.app.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.Before
import org.junit.Test
import org.surau.app.core.data.util.TimeZoneMonitor
import org.surau.app.core.datastore.AuthSession
import org.surau.app.core.datastore.AuthSessionDataSource
import org.surau.app.core.datastore.test.InMemoryDataStore
import org.surau.app.core.model.data.auth.AuthState
import org.surau.app.core.model.data.quran.AyahKey
import org.surau.app.core.model.data.quran.Bookmark
import org.surau.app.core.model.data.quran.QuranReadingPosition
import org.surau.app.core.network.model.SurauApiException
import org.surau.app.core.network.model.auth.ChangeEmailRequestDto
import org.surau.app.core.network.model.auth.ChangeEmailVerifyRequestDto
import org.surau.app.core.network.model.auth.ChangePasswordRequestDto
import org.surau.app.core.network.model.auth.DeleteAccountRequestDto
import org.surau.app.core.network.model.auth.ForgotPasswordRequestDto
import org.surau.app.core.network.model.auth.IntrospectDto
import org.surau.app.core.network.model.auth.LoginRequestDto
import org.surau.app.core.network.model.auth.LogoutRequestDto
import org.surau.app.core.network.model.auth.RefreshRequestDto
import org.surau.app.core.network.model.auth.RegisterRequestDto
import org.surau.app.core.network.model.auth.RegisteredUserDto
import org.surau.app.core.network.model.auth.ResendVerificationRequestDto
import org.surau.app.core.network.model.auth.ResetPasswordRequestDto
import org.surau.app.core.network.model.auth.SessionsResponseDto
import org.surau.app.core.network.model.auth.TokenPairDto
import org.surau.app.core.network.model.auth.VerifyEmailRequestDto
import org.surau.app.core.network.model.auth.VerifyEmailResponseDto
import org.surau.app.core.network.model.user.EmailPreferencesDto
import org.surau.app.core.network.model.user.EmailPreferencesPatchRequestDto
import org.surau.app.core.network.model.user.OnboardingRequestDto
import org.surau.app.core.network.model.user.PreferencesPatchRequestDto
import org.surau.app.core.network.model.user.ProfilePatchRequestDto
import org.surau.app.core.network.model.user.UserAccountDto
import org.surau.app.core.network.model.user.UserProfileDto
import org.surau.app.core.network.retrofit.SurauAccountApi
import org.surau.app.core.network.retrofit.SurauAuthApi
import org.surau.app.core.network.retrofit.SurauUserApi
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultAuthRepositoryTest {

    private val testScope = TestScope(UnconfinedTestDispatcher())

    private lateinit var authApi: FakeSurauAuthApi
    private lateinit var userApi: FakeSurauUserApi
    private lateinit var accountApi: FakeSurauAccountApi
    private lateinit var authSession: AuthSessionDataSource
    private lateinit var bookmarks: RecordingBookmarkRepository
    private lateinit var progress: RecordingQuranProgressRepository
    private lateinit var subject: DefaultAuthRepository

    @Before
    fun setup() {
        authApi = FakeSurauAuthApi()
        userApi = FakeSurauUserApi()
        accountApi = FakeSurauAccountApi()
        authSession = AuthSessionDataSource(InMemoryDataStore(AuthSession.getDefaultInstance()))
        bookmarks = RecordingBookmarkRepository()
        progress = RecordingQuranProgressRepository()
        subject = DefaultAuthRepository(
            authApi,
            userApi,
            accountApi,
            authSession,
            FakeTimeZoneMonitor(),
            bookmarks,
            progress,
        )
    }

    @Test
    fun login_persistsSession_thenEnrichesFromIntrospect() = testScope.runTest {
        authApi.loginResponse = TokenPairDto(
            accessToken = "access",
            refreshToken = "refresh",
            sessionId = "login-session",
            expiresInSeconds = 900,
        )
        userApi.introspectResponse = IntrospectDto(
            userId = "user-1",
            username = "Aisyah",
            sessionId = "introspect-session",
        )

        subject.login("user@surau.org", "secret")

        val state = subject.authState.first()
        assertIs<AuthState.Authenticated>(state)
        assertEquals("user-1", state.session.userId)
        assertEquals("Aisyah", state.session.displayName)
        assertEquals("introspect-session", state.session.sessionId)
        assertEquals("user@surau.org", state.session.email)
        assertEquals("access", authSession.currentAccessToken())
        assertEquals("refresh", authSession.currentRefreshToken())
    }

    @Test
    fun login_whenIntrospectFails_keepsMinimalSession() = testScope.runTest {
        authApi.loginResponse = TokenPairDto(
            accessToken = "access",
            refreshToken = "refresh",
            sessionId = "login-session",
        )
        userApi.introspectError = IOException("offline")

        subject.login("user@surau.org", "secret")

        val state = subject.authState.first()
        assertIs<AuthState.Authenticated>(state)
        // Minimal session retained from the token response; identity not enriched.
        assertEquals("", state.session.userId)
        assertNull(state.session.displayName)
        assertEquals("login-session", state.session.sessionId)
        assertEquals("user@surau.org", state.session.email)
        assertEquals("refresh", authSession.currentRefreshToken())
    }

    @Test
    fun logout_clearsLocalSession_evenWhenApiFails() = testScope.runTest {
        authApi.loginResponse = TokenPairDto(accessToken = "access", refreshToken = "refresh")
        subject.login("user@surau.org", "secret")
        authApi.logoutError = IOException("offline")

        subject.logout()

        // The API was attempted with the stored refresh token, yet the session is cleared anyway.
        assertEquals(listOf("refresh"), authApi.loggedOutRefreshTokens)
        assertEquals(AuthState.Guest, subject.authState.first())
        assertNull(authSession.currentAccessToken())
        // Identity-linked local data is wiped so the next user can't read or re-sync it.
        assertEquals(1, bookmarks.clearCount)
        assertEquals(1, progress.clearCount)
    }

    @Test
    fun logout_whenGuest_doesNotCallApi() = testScope.runTest {
        subject.logout()

        assertTrue(authApi.loggedOutRefreshTokens.isEmpty())
        assertEquals(AuthState.Guest, subject.authState.first())
    }

    @Test
    fun changePassword_persistsRotatedTokensAndSessionId() = testScope.runTest {
        signIn(email = "user@surau.org")
        accountApi.changePasswordResponse = TokenPairDto(
            accessToken = "rotated-access",
            refreshToken = "rotated-refresh",
            sessionId = "rotated-session",
        )

        subject.changePassword("oldsecret1", "newsecret1")

        assertEquals("rotated-access", authSession.currentAccessToken())
        assertEquals("rotated-refresh", authSession.currentRefreshToken())
        val state = assertIs<AuthState.Authenticated>(subject.authState.first())
        assertEquals("rotated-session", state.session.sessionId)
    }

    @Test
    fun verifyEmailChange_persistsTokensAndUpdatesEmail() = testScope.runTest {
        signIn(email = "old@surau.org")
        accountApi.verifyEmailChangeResponse = TokenPairDto(
            accessToken = "ec-access",
            refreshToken = "ec-refresh",
            sessionId = "ec-session",
        )

        subject.verifyEmailChange("new@surau.org", "123456")

        val state = assertIs<AuthState.Authenticated>(subject.authState.first())
        assertEquals("new@surau.org", state.session.email)
        assertEquals("ec-session", state.session.sessionId)
        assertEquals("ec-access", authSession.currentAccessToken())
    }

    @Test
    fun logoutAllDevices_clearsLocalSession() = testScope.runTest {
        signIn(email = "user@surau.org")

        subject.logoutAllDevices()

        assertEquals(1, accountApi.logoutAllCalls)
        assertEquals(AuthState.Guest, subject.authState.first())
        assertEquals(1, bookmarks.clearCount)
        assertEquals(1, progress.clearCount)
    }

    @Test
    fun deleteAccount_success_clearsLocalSession() = testScope.runTest {
        signIn(email = "user@surau.org")

        subject.deleteAccount("secret123")

        assertEquals(AuthState.Guest, subject.authState.first())
        assertEquals(1, bookmarks.clearCount)
        assertEquals(1, progress.clearCount)
    }

    @Test
    fun deleteAccount_wrongPassword_keepsSession() = testScope.runTest {
        signIn(email = "user@surau.org")
        accountApi.deleteAccountError = SurauApiException(
            httpStatus = 401,
            code = SurauApiException.CODE_INVALID_CREDENTIALS,
            message = "invalid credentials",
        )

        assertFailsWith<SurauApiException> { subject.deleteAccount("wrong") }

        // A wrong-password 401 must stay retryable: the session is NOT cleared.
        assertIs<AuthState.Authenticated>(subject.authState.first())
        assertEquals("access", authSession.currentAccessToken())
        // ...and local data is preserved while the account is still live.
        assertEquals(0, bookmarks.clearCount)
        assertEquals(0, progress.clearCount)
    }

    @Test
    fun updateProfile_sendsTimezone_andRefreshesDisplayName() = testScope.runTest {
        signIn(email = "user@surau.org")
        userApi.profileResponse = UserAccountDto(
            id = "user-1",
            profile = UserProfileDto(displayName = "Umar"),
        )

        subject.updateProfile("Umar", "ID")

        val patch = userApi.profilePatches.single()
        assertEquals("Umar", patch.displayName)
        assertEquals("ID", patch.countryCode)
        assertEquals("Asia/Jakarta", patch.timezone) // FakeTimeZoneMonitor default
        val state = assertIs<AuthState.Authenticated>(subject.authState.first())
        assertEquals("Umar", state.session.displayName)
    }

    private suspend fun signIn(email: String) {
        authApi.loginResponse = TokenPairDto(
            accessToken = "access",
            refreshToken = "refresh",
            sessionId = "login-session",
        )
        subject.login(email, "secret123")
    }
}

private class FakeSurauAuthApi : SurauAuthApi {
    var loginResponse: TokenPairDto = TokenPairDto(accessToken = "access", refreshToken = "refresh")
    var logoutError: Exception? = null
    val loggedOutRefreshTokens = mutableListOf<String>()

    override suspend fun login(body: LoginRequestDto): TokenPairDto = loginResponse

    override suspend fun logout(body: LogoutRequestDto) {
        // Record the attempt before failing, so tests can assert the call was made.
        loggedOutRefreshTokens += body.refreshToken
        logoutError?.let { throw it }
    }

    override suspend fun register(body: RegisterRequestDto): RegisteredUserDto =
        throw NotImplementedError()

    override suspend fun refresh(body: RefreshRequestDto): TokenPairDto = throw NotImplementedError()

    override suspend fun verifyEmail(body: VerifyEmailRequestDto): VerifyEmailResponseDto =
        throw NotImplementedError()

    override suspend fun resendVerification(body: ResendVerificationRequestDto) =
        throw NotImplementedError()

    override suspend fun forgotPassword(body: ForgotPasswordRequestDto) = throw NotImplementedError()

    override suspend fun resetPassword(body: ResetPasswordRequestDto) = throw NotImplementedError()
}

private class FakeSurauUserApi : SurauUserApi {
    var introspectResponse: IntrospectDto = IntrospectDto(userId = "user-1")
    var introspectError: Exception? = null

    override suspend fun introspect(): IntrospectDto =
        introspectError?.let { throw it } ?: introspectResponse

    override suspend fun profile(): UserAccountDto = throw NotImplementedError()

    override suspend fun completeOnboarding(body: OnboardingRequestDto): UserAccountDto =
        throw NotImplementedError()

    override suspend fun patchPreferences(body: PreferencesPatchRequestDto): UserAccountDto =
        throw NotImplementedError()

    var profileResponse: UserAccountDto = UserAccountDto(id = "user-1")
    var emailPreferencesResponse: EmailPreferencesDto = EmailPreferencesDto(marketingOptIn = false)
    val profilePatches = mutableListOf<ProfilePatchRequestDto>()

    override suspend fun updateProfile(body: ProfilePatchRequestDto): UserAccountDto {
        profilePatches += body
        return profileResponse
    }

    override suspend fun emailPreferences(): EmailPreferencesDto = emailPreferencesResponse

    override suspend fun updateEmailPreferences(
        body: EmailPreferencesPatchRequestDto,
    ): EmailPreferencesDto = EmailPreferencesDto(marketingOptIn = body.marketingOptIn)
}

private class FakeSurauAccountApi : SurauAccountApi {
    var changePasswordResponse: TokenPairDto = ROTATED_PAIR
    var changePasswordError: Exception? = null
    var requestEmailChangeError: Exception? = null
    var verifyEmailChangeResponse: TokenPairDto = ROTATED_PAIR
    var verifyEmailChangeError: Exception? = null
    var deleteAccountError: Exception? = null
    var sessionsResponse: SessionsResponseDto = SessionsResponseDto()
    val revokedSessionIds = mutableListOf<String>()
    var logoutAllCalls = 0

    override suspend fun changePassword(body: ChangePasswordRequestDto): TokenPairDto =
        changePasswordError?.let { throw it } ?: changePasswordResponse

    override suspend fun requestEmailChange(body: ChangeEmailRequestDto) {
        requestEmailChangeError?.let { throw it }
    }

    override suspend fun verifyEmailChange(body: ChangeEmailVerifyRequestDto): TokenPairDto =
        verifyEmailChangeError?.let { throw it } ?: verifyEmailChangeResponse

    override suspend fun listSessions(): SessionsResponseDto = sessionsResponse

    override suspend fun revokeSession(id: String) {
        revokedSessionIds += id
    }

    override suspend fun logoutAll() {
        logoutAllCalls++
    }

    override suspend fun deleteAccount(body: DeleteAccountRequestDto) {
        deleteAccountError?.let { throw it }
    }

    private companion object {
        val ROTATED_PAIR = TokenPairDto(
            accessToken = "rotated-access",
            refreshToken = "rotated-refresh",
            sessionId = "rotated-session",
            expiresInSeconds = 900,
        )
    }
}

private class FakeTimeZoneMonitor(
    zone: TimeZone = TimeZone.of("Asia/Jakarta"),
) : TimeZoneMonitor {
    override val currentTimeZone: Flow<TimeZone> = flowOf(zone)
}

/** Records [clearLocalData] calls; every other method is unused by the auth repository. */
private class RecordingBookmarkRepository : BookmarkRepository {
    var clearCount = 0
        private set

    override suspend fun clearLocalData() {
        clearCount++
    }

    override fun observeBookmarks(): Flow<List<Bookmark>> = throw NotImplementedError()
    override fun observeBookmark(ayahKey: AyahKey): Flow<Bookmark?> = throw NotImplementedError()
    override fun observeTags(): Flow<List<String>> = throw NotImplementedError()
    override suspend fun addBookmark(ayahKey: AyahKey, note: String?, tags: List<String>) =
        throw NotImplementedError()
    override suspend fun updateBookmark(ayahKey: AyahKey, note: String?, tags: List<String>) =
        throw NotImplementedError()
    override suspend fun removeBookmark(ayahKey: AyahKey) = throw NotImplementedError()
    override suspend fun pushPending() = throw NotImplementedError()
    override suspend fun reconcile() = throw NotImplementedError()
}

/** Records [clearLocalData] calls; every other method is unused by the auth repository. */
private class RecordingQuranProgressRepository : QuranProgressRepository {
    var clearCount = 0
        private set

    override suspend fun clearLocalData() {
        clearCount++
    }

    override fun observePosition(): Flow<QuranReadingPosition?> = throw NotImplementedError()
    override suspend fun savePosition(ayahKey: AyahKey) = throw NotImplementedError()
    override suspend fun pushPendingPosition() = throw NotImplementedError()
    override suspend fun reconcile() = throw NotImplementedError()
}
