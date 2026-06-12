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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.surau.app.core.designsystem.component.SurauButton
import org.surau.app.core.designsystem.component.SurauOutlinedButton
import org.surau.app.core.designsystem.component.SurauTextButton
import org.surau.app.core.ui.TrackScreenViewEvent

@Composable
fun WelcomeScreen(
    onContinueAsGuest: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WelcomeViewModel = hiltViewModel(),
) {
    TrackScreenViewEvent(screenName = "Welcome")
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.feature_auth_impl_welcome_title),
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = stringResource(R.string.feature_auth_impl_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.size(40.dp))
        SurauButton(
            onClick = {
                viewModel.markWelcomeShown()
                onContinueAsGuest()
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("welcome:guest"),
        ) {
            Text(stringResource(R.string.feature_auth_impl_continue_as_guest))
        }
        Spacer(modifier = Modifier.size(12.dp))
        SurauOutlinedButton(
            onClick = {
                viewModel.markWelcomeShown()
                onSignIn()
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("welcome:signIn"),
        ) {
            Text(stringResource(R.string.feature_auth_impl_sign_in))
        }
    }
}

@Composable
fun LoginScreen(
    onBackClick: () -> Unit,
    onLoggedIn: () -> Unit,
    onNeedsVerification: (email: String) -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    TrackScreenViewEvent(screenName = "Login")
    val submitState by viewModel.submitState.collectAsStateWithLifecycle()

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showValidation by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(submitState) {
        when (val state = submitState) {
            AuthSubmitState.Success -> {
                viewModel.consumeNavigation()
                onLoggedIn()
            }

            is AuthSubmitState.RequiresVerification -> {
                viewModel.consumeNavigation()
                onNeedsVerification(state.email)
            }

            else -> Unit
        }
    }

    AuthScreenScaffold(
        title = stringResource(R.string.feature_auth_impl_sign_in_title),
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        EmailField(
            value = email,
            onValueChange = { email = it },
            isError = showValidation && !AuthValidators.isValidEmail(email),
            modifier = Modifier.testTag("login:email"),
        )
        Spacer(modifier = Modifier.size(12.dp))
        PasswordField(
            value = password,
            onValueChange = { password = it },
            isError = showValidation && !AuthValidators.isValidPassword(password),
            modifier = Modifier.testTag("login:password"),
        )
        SurauTextButton(onClick = onForgotPasswordClick) {
            Text(stringResource(R.string.feature_auth_impl_forgot_password))
        }

        AuthSubmitError(submitState)

        Spacer(modifier = Modifier.size(16.dp))
        SurauButton(
            onClick = {
                showValidation = true
                if (AuthValidators.isValidEmail(email) &&
                    AuthValidators.isValidPassword(password)
                ) {
                    viewModel.login(email, password)
                }
            },
            enabled = submitState !is AuthSubmitState.Submitting &&
                submitState !is AuthSubmitState.RateLimited,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login:submit"),
        ) {
            Text(stringResource(R.string.feature_auth_impl_sign_in))
        }

        Spacer(modifier = Modifier.size(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.feature_auth_impl_no_account),
                style = MaterialTheme.typography.bodyMedium,
            )
            SurauTextButton(onClick = onRegisterClick) {
                Text(stringResource(R.string.feature_auth_impl_register))
            }
        }
    }
}

@Composable
fun RegisterScreen(
    onBackClick: () -> Unit,
    onNeedsVerification: (email: String) -> Unit,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    TrackScreenViewEvent(screenName = "Register")
    val submitState by viewModel.submitState.collectAsStateWithLifecycle()

    var displayName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showValidation by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(submitState) {
        val state = submitState
        if (state is AuthSubmitState.RequiresVerification) {
            viewModel.consumeNavigation()
            onNeedsVerification(state.email)
        }
    }

    AuthScreenScaffold(
        title = stringResource(R.string.feature_auth_impl_register_title),
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text(stringResource(R.string.feature_auth_impl_display_name)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register:name"),
        )
        Spacer(modifier = Modifier.size(12.dp))
        EmailField(
            value = email,
            onValueChange = { email = it },
            isError = showValidation && !AuthValidators.isValidEmail(email),
            modifier = Modifier.testTag("register:email"),
        )
        Spacer(modifier = Modifier.size(12.dp))
        PasswordField(
            value = password,
            onValueChange = { password = it },
            isError = showValidation && !AuthValidators.isValidPassword(password),
            modifier = Modifier.testTag("register:password"),
        )

        AuthSubmitError(submitState)

        Spacer(modifier = Modifier.size(16.dp))
        SurauButton(
            onClick = {
                showValidation = true
                if (AuthValidators.isValidEmail(email) &&
                    AuthValidators.isValidPassword(password)
                ) {
                    viewModel.register(email, password, displayName)
                }
            },
            enabled = submitState !is AuthSubmitState.Submitting &&
                submitState !is AuthSubmitState.RateLimited,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register:submit"),
        ) {
            Text(stringResource(R.string.feature_auth_impl_register))
        }

        Spacer(modifier = Modifier.size(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.feature_auth_impl_have_account),
                style = MaterialTheme.typography.bodyMedium,
            )
            SurauTextButton(onClick = onSignInClick) {
                Text(stringResource(R.string.feature_auth_impl_sign_in))
            }
        }
    }
}

@Composable
fun VerifyEmailScreen(
    email: String,
    onBackClick: () -> Unit,
    onVerified: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VerifyEmailViewModel =
        hiltViewModel<VerifyEmailViewModel, VerifyEmailViewModel.Factory>(
            key = "verify:$email",
        ) { factory ->
            factory.create(email)
        },
) {
    TrackScreenViewEvent(screenName = "VerifyEmail")
    val submitState by viewModel.submitState.collectAsStateWithLifecycle()
    val resendCooldown by viewModel.resendCooldownSeconds.collectAsStateWithLifecycle()

    var otp by rememberSaveable { mutableStateOf("") }
    var showValidation by rememberSaveable { mutableStateOf(false) }

    if (submitState is AuthSubmitState.Success) {
        AuthScreenScaffold(
            title = stringResource(R.string.feature_auth_impl_verified_title),
            onBackClick = null,
            subtitle = stringResource(R.string.feature_auth_impl_verified_subtitle),
            modifier = modifier,
        ) {
            SurauButton(
                onClick = onVerified,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("verify:done"),
            ) {
                Text(stringResource(R.string.feature_auth_impl_sign_in))
            }
        }
        return
    }

    AuthScreenScaffold(
        title = stringResource(R.string.feature_auth_impl_verify_title),
        onBackClick = onBackClick,
        subtitle = stringResource(R.string.feature_auth_impl_verify_subtitle, email),
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = otp,
            onValueChange = { if (it.length <= 6) otp = it },
            label = { Text(stringResource(R.string.feature_auth_impl_otp)) },
            isError = showValidation && !AuthValidators.isValidOtp(otp),
            supportingText = if (showValidation && !AuthValidators.isValidOtp(otp)) {
                { Text(stringResource(R.string.feature_auth_impl_otp_invalid)) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("verify:otp"),
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
                .testTag("verify:submit"),
        ) {
            Text(stringResource(R.string.feature_auth_impl_verify))
        }

        Spacer(modifier = Modifier.size(8.dp))
        SurauTextButton(
            onClick = viewModel::resend,
            enabled = resendCooldown == 0L,
            modifier = Modifier.testTag("verify:resend"),
        ) {
            Text(
                text = if (resendCooldown > 0) {
                    stringResource(R.string.feature_auth_impl_resend_in, resendCooldown)
                } else {
                    stringResource(R.string.feature_auth_impl_resend)
                },
            )
        }
    }
}

@Composable
fun ForgotPasswordScreen(
    onBackClick: () -> Unit,
    onContinueToReset: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    TrackScreenViewEvent(screenName = "ForgotPassword")
    val submitState by viewModel.submitState.collectAsStateWithLifecycle()

    var email by rememberSaveable { mutableStateOf("") }
    var showValidation by rememberSaveable { mutableStateOf(false) }

    AuthScreenScaffold(
        title = stringResource(R.string.feature_auth_impl_forgot_title),
        onBackClick = onBackClick,
        subtitle = stringResource(R.string.feature_auth_impl_forgot_subtitle),
        modifier = modifier,
    ) {
        EmailField(
            value = email,
            onValueChange = { email = it },
            isError = showValidation && !AuthValidators.isValidEmail(email),
            modifier = Modifier.testTag("forgot:email"),
        )

        AuthSubmitError(submitState)

        if (submitState is AuthSubmitState.Success) {
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.feature_auth_impl_forgot_sent),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(modifier = Modifier.size(16.dp))
        SurauButton(
            onClick = {
                showValidation = true
                if (AuthValidators.isValidEmail(email)) viewModel.sendResetEmail(email)
            },
            enabled = submitState !is AuthSubmitState.Submitting,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("forgot:submit"),
        ) {
            Text(stringResource(R.string.feature_auth_impl_send))
        }

        Spacer(modifier = Modifier.size(8.dp))
        SurauTextButton(onClick = onContinueToReset) {
            Text(stringResource(R.string.feature_auth_impl_enter_token))
        }
    }
}

@Composable
fun ResetPasswordScreen(
    initialToken: String?,
    onBackClick: () -> Unit,
    onResetDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResetPasswordViewModel = hiltViewModel(),
) {
    TrackScreenViewEvent(screenName = "ResetPassword")
    val submitState by viewModel.submitState.collectAsStateWithLifecycle()

    var token by rememberSaveable { mutableStateOf(initialToken.orEmpty()) }
    var password by rememberSaveable { mutableStateOf("") }
    var showValidation by rememberSaveable { mutableStateOf(false) }

    if (submitState is AuthSubmitState.Success) {
        AuthScreenScaffold(
            title = stringResource(R.string.feature_auth_impl_reset_title),
            onBackClick = null,
            subtitle = stringResource(R.string.feature_auth_impl_reset_done),
            modifier = modifier,
        ) {
            SurauButton(
                onClick = onResetDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reset:done"),
            ) {
                Text(stringResource(R.string.feature_auth_impl_sign_in))
            }
        }
        return
    }

    AuthScreenScaffold(
        title = stringResource(R.string.feature_auth_impl_reset_title),
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text(stringResource(R.string.feature_auth_impl_reset_token)) },
            isError = showValidation && token.isBlank(),
            supportingText = if (showValidation && token.isBlank()) {
                { Text(stringResource(R.string.feature_auth_impl_token_required)) }
            } else {
                null
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reset:token"),
        )
        Spacer(modifier = Modifier.size(12.dp))
        PasswordField(
            value = password,
            onValueChange = { password = it },
            isError = showValidation && !AuthValidators.isValidPassword(password),
            label = stringResource(R.string.feature_auth_impl_new_password),
            modifier = Modifier.testTag("reset:password"),
        )

        AuthSubmitError(submitState)

        Spacer(modifier = Modifier.size(16.dp))
        SurauButton(
            onClick = {
                showValidation = true
                if (token.isNotBlank() && AuthValidators.isValidPassword(password)) {
                    viewModel.resetPassword(token, password)
                }
            },
            enabled = submitState !is AuthSubmitState.Submitting,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reset:submit"),
        ) {
            Text(stringResource(R.string.feature_auth_impl_reset))
        }
    }
}
