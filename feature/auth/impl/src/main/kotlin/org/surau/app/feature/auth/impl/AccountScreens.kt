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

package org.surau.app.feature.auth.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.surau.app.core.designsystem.component.SurauButton
import org.surau.app.core.designsystem.component.SurauLoadingWheel
import org.surau.app.core.designsystem.component.SurauOutlinedButton
import org.surau.app.core.designsystem.component.SurauSwitch
import org.surau.app.core.designsystem.component.SurauTextButton
import org.surau.app.core.designsystem.icon.SurauIcons
import org.surau.app.core.model.data.auth.AccountSession
import org.surau.app.core.model.data.auth.AuthState
import org.surau.app.core.ui.TrackScreenViewEvent

@Composable
fun AccountScreen(
    onBackClick: () -> Unit,
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onChangeEmail: () -> Unit,
    onSessions: () -> Unit,
    onEmailPreferences: () -> Unit,
    onDeleteAccount: () -> Unit,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    TrackScreenViewEvent(screenName = "Account")
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val signedOut by viewModel.signedOut.collectAsStateWithLifecycle()
    var showLogoutAllConfirm by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffectSignedOut(signedOut, onSignedOut)
    LaunchedEffect(Unit) {
        viewModel.errors.collect { resId -> snackbarHostState.showSnackbar(resources.getString(resId)) }
    }

    val session = (authState as? AuthState.Authenticated)?.session

    Box(modifier = modifier.fillMaxSize()) {
        AuthScreenScaffold(
            title = stringResource(R.string.feature_auth_impl_account_title),
            onBackClick = onBackClick,
            subtitle = session?.let { it.displayName ?: it.email },
        ) {
            AccountRow(
                label = stringResource(R.string.feature_auth_impl_account_profile),
                onClick = onEditProfile,
                modifier = Modifier.testTag("account:profile"),
            )
            AccountRow(
                label = stringResource(R.string.feature_auth_impl_account_change_password),
                onClick = onChangePassword,
                modifier = Modifier.testTag("account:password"),
            )
            AccountRow(
                label = stringResource(R.string.feature_auth_impl_account_change_email),
                onClick = onChangeEmail,
                modifier = Modifier.testTag("account:email"),
            )
            AccountRow(
                label = stringResource(R.string.feature_auth_impl_account_sessions),
                onClick = onSessions,
                modifier = Modifier.testTag("account:sessions"),
            )
            AccountRow(
                label = stringResource(R.string.feature_auth_impl_account_email_prefs),
                onClick = onEmailPreferences,
                modifier = Modifier.testTag("account:emailPrefs"),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            AccountRow(
                label = stringResource(R.string.feature_auth_impl_account_logout_all),
                onClick = { showLogoutAllConfirm = true },
                modifier = Modifier.testTag("account:logoutAll"),
            )
            AccountRow(
                label = stringResource(R.string.feature_auth_impl_account_delete),
                onClick = onDeleteAccount,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("account:delete"),
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }

    if (showLogoutAllConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutAllConfirm = false },
            title = { Text(stringResource(R.string.feature_auth_impl_account_logout_all_confirm_title)) },
            text = { Text(stringResource(R.string.feature_auth_impl_account_logout_all_confirm_body)) },
            confirmButton = {
                SurauTextButton(
                    onClick = {
                        showLogoutAllConfirm = false
                        viewModel.logoutAllDevices()
                    },
                    modifier = Modifier.testTag("account:logoutAllConfirm"),
                ) {
                    Text(stringResource(R.string.feature_auth_impl_account_logout_all))
                }
            },
            dismissButton = {
                SurauTextButton(onClick = { showLogoutAllConfirm = false }) {
                    Text(stringResource(R.string.feature_auth_impl_account_cancel))
                }
            },
        )
    }
}

@Composable
fun EditProfileScreen(
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    TrackScreenViewEvent(screenName = "EditProfile")
    val submitState by viewModel.submitState.collectAsStateWithLifecycle()
    val currentDisplayName by viewModel.currentDisplayName.collectAsStateWithLifecycle()

    var displayName by rememberSaveable { mutableStateOf("") }
    var countryCode by rememberSaveable { mutableStateOf("") }
    var seeded by rememberSaveable { mutableStateOf(false) }
    var showValidation by rememberSaveable { mutableStateOf(false) }

    SeedOnce(seeded, currentDisplayName) {
        displayName = it
        seeded = true
    }
    NavigateOnSuccess(submitState, onSaved)

    AuthScreenScaffold(
        title = stringResource(R.string.feature_auth_impl_account_profile_title),
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text(stringResource(R.string.feature_auth_impl_display_name)) },
            isError = showValidation && displayName.isBlank(),
            supportingText = if (showValidation && displayName.isBlank()) {
                { Text(stringResource(R.string.feature_auth_impl_account_name_required)) }
            } else {
                null
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile:name"),
        )
        Spacer(modifier = Modifier.size(12.dp))
        OutlinedTextField(
            value = countryCode,
            onValueChange = { if (it.length <= 2) countryCode = it.uppercase() },
            label = { Text(stringResource(R.string.feature_auth_impl_account_country_code)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile:country"),
        )

        AuthSubmitError(submitState)

        Spacer(modifier = Modifier.size(16.dp))
        SurauButton(
            onClick = {
                showValidation = true
                if (displayName.isNotBlank()) viewModel.save(displayName, countryCode)
            },
            enabled = submitState !is AuthSubmitState.Submitting,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile:save"),
        ) {
            Text(stringResource(R.string.feature_auth_impl_account_save))
        }
    }
}

@Composable
fun ChangePasswordScreen(
    onBackClick: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChangePasswordViewModel = hiltViewModel(),
) {
    TrackScreenViewEvent(screenName = "ChangePassword")
    val submitState by viewModel.submitState.collectAsStateWithLifecycle()

    var currentPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var showValidation by rememberSaveable { mutableStateOf(false) }

    if (submitState is AuthSubmitState.Success) {
        SuccessScreen(
            title = stringResource(R.string.feature_auth_impl_account_password_title),
            subtitle = stringResource(R.string.feature_auth_impl_account_password_done),
            onDone = onDone,
            doneTestTag = "changePassword:done",
            modifier = modifier,
        )
        return
    }

    AuthScreenScaffold(
        title = stringResource(R.string.feature_auth_impl_account_password_title),
        onBackClick = onBackClick,
        subtitle = stringResource(R.string.feature_auth_impl_account_password_subtitle),
        modifier = modifier,
    ) {
        PasswordField(
            value = currentPassword,
            onValueChange = { currentPassword = it },
            isError = false,
            label = stringResource(R.string.feature_auth_impl_account_current_password),
            modifier = Modifier.testTag("changePassword:current"),
        )
        Spacer(modifier = Modifier.size(12.dp))
        PasswordField(
            value = newPassword,
            onValueChange = { newPassword = it },
            isError = showValidation && !AuthValidators.isValidPassword(newPassword),
            label = stringResource(R.string.feature_auth_impl_new_password),
            modifier = Modifier.testTag("changePassword:new"),
        )

        AuthSubmitError(submitState)

        Spacer(modifier = Modifier.size(16.dp))
        SurauButton(
            onClick = {
                showValidation = true
                if (currentPassword.isNotBlank() && AuthValidators.isValidPassword(newPassword)) {
                    viewModel.changePassword(currentPassword, newPassword)
                }
            },
            enabled = submitState !is AuthSubmitState.Submitting &&
                submitState !is AuthSubmitState.RateLimited,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("changePassword:submit"),
        ) {
            Text(stringResource(R.string.feature_auth_impl_account_save))
        }
    }
}

@Composable
fun ChangeEmailScreen(
    onBackClick: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChangeEmailViewModel = hiltViewModel(),
) {
    TrackScreenViewEvent(screenName = "ChangeEmail")
    val submitState by viewModel.submitState.collectAsStateWithLifecycle()
    val awaitingOtp by viewModel.awaitingOtp.collectAsStateWithLifecycle()
    val newEmail by viewModel.newEmail.collectAsStateWithLifecycle()

    var currentPassword by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var otp by rememberSaveable { mutableStateOf("") }
    var showValidation by rememberSaveable { mutableStateOf(false) }

    if (submitState is AuthSubmitState.Success) {
        SuccessScreen(
            title = stringResource(R.string.feature_auth_impl_account_email_title),
            subtitle = stringResource(R.string.feature_auth_impl_account_email_done, newEmail),
            onDone = onDone,
            doneTestTag = "changeEmail:done",
            modifier = modifier,
        )
        return
    }

    if (awaitingOtp) {
        AuthScreenScaffold(
            title = stringResource(R.string.feature_auth_impl_account_email_title),
            onBackClick = viewModel::restartEmailChange,
            subtitle = stringResource(R.string.feature_auth_impl_verify_subtitle, newEmail),
            modifier = modifier,
        ) {
            OtpField(
                otp = otp,
                onOtpChange = { if (it.length <= 6) otp = it },
                showError = showValidation && !AuthValidators.isValidOtp(otp),
                testTag = "changeEmail:otp",
            )

            AuthSubmitError(submitState)

            Spacer(modifier = Modifier.size(16.dp))
            SurauButton(
                onClick = {
                    showValidation = true
                    if (AuthValidators.isValidOtp(otp)) viewModel.verify(otp)
                },
                enabled = submitState !is AuthSubmitState.Submitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("changeEmail:verify"),
            ) {
                Text(stringResource(R.string.feature_auth_impl_verify))
            }
        }
        return
    }

    AuthScreenScaffold(
        title = stringResource(R.string.feature_auth_impl_account_email_title),
        onBackClick = onBackClick,
        subtitle = stringResource(R.string.feature_auth_impl_account_email_subtitle),
        modifier = modifier,
    ) {
        PasswordField(
            value = currentPassword,
            onValueChange = { currentPassword = it },
            isError = false,
            label = stringResource(R.string.feature_auth_impl_account_current_password),
            modifier = Modifier.testTag("changeEmail:current"),
        )
        Spacer(modifier = Modifier.size(12.dp))
        EmailField(
            value = email,
            onValueChange = { email = it },
            isError = showValidation && !AuthValidators.isValidEmail(email),
            modifier = Modifier.testTag("changeEmail:newEmail"),
        )

        AuthSubmitError(submitState)

        Spacer(modifier = Modifier.size(16.dp))
        SurauButton(
            onClick = {
                showValidation = true
                if (currentPassword.isNotBlank() && AuthValidators.isValidEmail(email)) {
                    viewModel.requestChange(currentPassword, email)
                }
            },
            enabled = submitState !is AuthSubmitState.Submitting &&
                submitState !is AuthSubmitState.RateLimited,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("changeEmail:submit"),
        ) {
            Text(stringResource(R.string.feature_auth_impl_account_email_send_code))
        }
    }
}

@Composable
fun SessionsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionsViewModel = hiltViewModel(),
) {
    TrackScreenViewEvent(screenName = "Sessions")
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(Unit) {
        viewModel.errors.collect { resId -> snackbarHostState.showSnackbar(resources.getString(resId)) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AuthScreenScaffold(
            title = stringResource(R.string.feature_auth_impl_account_sessions_title),
            onBackClick = onBackClick,
            subtitle = stringResource(R.string.feature_auth_impl_account_sessions_subtitle),
        ) {
            when (val state = uiState) {
                SessionsUiState.Loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    SurauLoadingWheel(
                        contentDesc = stringResource(R.string.feature_auth_impl_account_sessions_title),
                    )
                }

                SessionsUiState.Error -> RetryableError(
                    message = stringResource(R.string.feature_auth_impl_account_sessions_error),
                    onRetry = viewModel::load,
                    retryTestTag = "sessions:retry",
                )

                is SessionsUiState.Success -> state.sessions.forEachIndexed { index, session ->
                    if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SessionRow(session = session, onRevoke = { viewModel.revoke(session.id) })
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

@Composable
fun EmailPreferencesScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EmailPreferencesViewModel = hiltViewModel(),
) {
    TrackScreenViewEvent(screenName = "EmailPreferences")
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AuthScreenScaffold(
        title = stringResource(R.string.feature_auth_impl_account_email_prefs_title),
        onBackClick = onBackClick,
        subtitle = stringResource(R.string.feature_auth_impl_account_email_prefs_subtitle),
        modifier = modifier,
    ) {
        when (val state = uiState) {
            EmailPrefsUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                SurauLoadingWheel(
                    contentDesc = stringResource(R.string.feature_auth_impl_account_email_prefs_title),
                )
            }

            EmailPrefsUiState.Error -> RetryableError(
                message = stringResource(R.string.feature_auth_impl_account_email_prefs_error),
                onRetry = viewModel::load,
                retryTestTag = "emailPrefs:retry",
            )

            is EmailPrefsUiState.Success -> Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.feature_auth_impl_account_email_marketing),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                SurauSwitch(
                    checked = state.marketingOptIn,
                    onCheckedChange = viewModel::setMarketingOptIn,
                    modifier = Modifier.testTag("emailPrefs:marketing"),
                )
            }
        }
    }
}

@Composable
fun DeleteAccountScreen(
    onBackClick: () -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeleteAccountViewModel = hiltViewModel(),
) {
    TrackScreenViewEvent(screenName = "DeleteAccount")
    val submitState by viewModel.submitState.collectAsStateWithLifecycle()

    var password by rememberSaveable { mutableStateOf("") }
    var confirmText by rememberSaveable { mutableStateOf("") }
    var showValidation by rememberSaveable { mutableStateOf(false) }

    NavigateOnSuccess(submitState, onDeleted)

    val confirmWord = stringResource(R.string.feature_auth_impl_account_delete_confirm_word)
    val canDelete = password.isNotBlank() && confirmText.trim() == confirmWord

    AuthScreenScaffold(
        title = stringResource(R.string.feature_auth_impl_account_delete_title),
        onBackClick = onBackClick,
        subtitle = stringResource(R.string.feature_auth_impl_account_delete_subtitle),
        modifier = modifier,
    ) {
        PasswordField(
            value = password,
            onValueChange = { password = it },
            isError = false,
            label = stringResource(R.string.feature_auth_impl_account_current_password),
            modifier = Modifier.testTag("deleteAccount:password"),
        )
        Spacer(modifier = Modifier.size(12.dp))
        OutlinedTextField(
            value = confirmText,
            onValueChange = { confirmText = it },
            label = {
                Text(stringResource(R.string.feature_auth_impl_account_delete_confirm_label, confirmWord))
            },
            isError = showValidation && confirmText.trim() != confirmWord,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("deleteAccount:confirm"),
        )

        AuthSubmitError(submitState)

        Spacer(modifier = Modifier.size(16.dp))
        SurauButton(
            onClick = {
                showValidation = true
                if (canDelete) viewModel.deleteAccount(password)
            },
            enabled = canDelete &&
                submitState !is AuthSubmitState.Submitting &&
                submitState !is AuthSubmitState.RateLimited,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("deleteAccount:submit"),
        ) {
            Text(stringResource(R.string.feature_auth_impl_account_delete))
        }
    }
}

// region shared building blocks

@Composable
private fun SuccessScreen(
    title: String,
    subtitle: String,
    onDone: () -> Unit,
    doneTestTag: String,
    modifier: Modifier = Modifier,
) {
    AuthScreenScaffold(
        title = title,
        onBackClick = null,
        subtitle = subtitle,
        modifier = modifier,
    ) {
        SurauButton(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(doneTestTag),
        ) {
            Text(stringResource(R.string.feature_auth_impl_account_done))
        }
    }
}

@Composable
private fun RetryableError(
    message: String,
    onRetry: () -> Unit,
    retryTestTag: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.size(12.dp))
        SurauOutlinedButton(
            onClick = onRetry,
            modifier = Modifier.testTag(retryTestTag),
        ) {
            Text(stringResource(R.string.feature_auth_impl_account_retry))
        }
    }
}

@Composable
internal fun AccountRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = color,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = SurauIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun SessionRow(
    session: AccountSession,
    onRevoke: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.userAgent.ifBlank {
                    stringResource(R.string.feature_auth_impl_account_session_unknown)
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            val lastUsed = session.lastUsedAt?.toString()?.take(10)
            val detail = listOfNotNull(session.clientIp.ifBlank { null }, lastUsed)
                .joinToString(" · ")
            if (detail.isNotEmpty()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (session.isCurrent) {
                Text(
                    text = stringResource(R.string.feature_auth_impl_account_session_current),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (!session.isCurrent) {
            SurauOutlinedButton(
                onClick = onRevoke,
                modifier = Modifier.testTag("sessions:revoke:${session.id}"),
            ) {
                Text(stringResource(R.string.feature_auth_impl_account_session_revoke))
            }
        }
    }
}

@Composable
private fun OtpField(
    otp: String,
    onOtpChange: (String) -> Unit,
    showError: Boolean,
    testTag: String,
) {
    OutlinedTextField(
        value = otp,
        onValueChange = onOtpChange,
        label = { Text(stringResource(R.string.feature_auth_impl_otp)) },
        isError = showError,
        supportingText = if (showError) {
            { Text(stringResource(R.string.feature_auth_impl_otp_invalid)) }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
    )
}

@Composable
private fun NavigateOnSuccess(state: AuthSubmitState, onSuccess: () -> Unit) {
    LaunchedEffect(state) {
        if (state is AuthSubmitState.Success) onSuccess()
    }
}

@Composable
private fun LaunchedEffectSignedOut(signedOut: Boolean, onSignedOut: () -> Unit) {
    LaunchedEffect(signedOut) {
        if (signedOut) onSignedOut()
    }
}

@Composable
private fun SeedOnce(seeded: Boolean, value: String?, onSeed: (String) -> Unit) {
    LaunchedEffect(value) {
        if (!seeded && value != null) onSeed(value)
    }
}

// endregion
