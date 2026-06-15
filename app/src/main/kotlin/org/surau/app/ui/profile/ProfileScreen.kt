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

package org.surau.app.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.surau.app.R
import org.surau.app.core.designsystem.icon.SurauIcons
import org.surau.app.core.model.data.auth.AuthState

@Composable
fun ProfileScreen(
    appVersionName: String,
    onManageAccount: () -> Unit,
    onSettings: () -> Unit,
    onActivity: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val signedIn = authState is AuthState.Authenticated
    val session = (authState as? AuthState.Authenticated)?.session

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Account header
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(56.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = SurauIcons.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session?.displayName?.takeIf { it.isNotBlank() }
                            ?: session?.email
                            ?: stringResource(R.string.profile_guest),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = if (signedIn) {
                            session?.email.orEmpty()
                        } else {
                            stringResource(R.string.profile_guest_body)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (!signedIn) {
            Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.profile_sign_in))
            }
        }

        // Links
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                if (signedIn) {
                    ProfileRow(SurauIcons.Person, stringResource(R.string.profile_manage_account), onManageAccount)
                    HorizontalDivider()
                }
                ProfileRow(SurauIcons.Activity, stringResource(R.string.profile_activity), onActivity)
                HorizontalDivider()
                ProfileRow(SurauIcons.Settings, stringResource(R.string.profile_settings), onSettings)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.profile_version, appVersionName),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ProfileRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingContent = {
            Icon(SurauIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
