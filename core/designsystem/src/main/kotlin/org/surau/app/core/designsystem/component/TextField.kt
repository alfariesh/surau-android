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

package org.surau.app.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.surau.app.core.designsystem.theme.LocalSurauColors
import org.surau.app.core.designsystem.theme.SurauTheme

/**
 * The single text-field style used across Surau: a **filled** field that shows **no border at rest**
 * and a **full** accent border on **all four sides** when focused (not the Material filled-field
 * bottom indicator). Built on [OutlinedTextField] because its outline draws a full rounded box that
 * wraps only the input — supporting text stays outside the border — which is exactly the geometry we
 * want; we just hide the resting outline and tint the container so it reads as filled.
 *
 * The parameter set mirrors [OutlinedTextField] so it drops in at every call site. [modifier] is
 * passed straight to the field, so `fillMaxWidth`/`weight`/`testTag`/`focusRequester` keep working.
 *
 * @param isError when `true` the border shows in the danger colour regardless of focus.
 * @param supportingText optional line rendered below the field (outside the border), e.g. an error.
 */
@Composable
fun SurauTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    shape: Shape = RoundedCornerShape(SurauTextFieldDefaults.CornerRadius),
    colors: TextFieldColors = SurauTextFieldDefaults.colors(),
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = isError,
        supportingText = supportingText,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        shape = shape,
        colors = colors,
    )
}

/** Defaults for [SurauTextField]. */
object SurauTextFieldDefaults {
    /** Corner radius of the filled container and the focus border. */
    val CornerRadius = 18.dp

    /**
     * Filled container with the resting outline hidden (transparent) and a full accent outline on
     * focus. Error switches the outline to the danger colour; the cursor and focused label use the
     * accent.
     */
    @Composable
    fun colors(): TextFieldColors {
        val c = LocalSurauColors.current
        return OutlinedTextFieldDefaults.colors(
            focusedContainerColor = c.surfaceSecondary,
            unfocusedContainerColor = c.surfaceSecondary,
            disabledContainerColor = c.surfaceSecondary,
            errorContainerColor = c.surfaceSecondary,
            focusedBorderColor = c.accent,
            unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
            errorBorderColor = c.danger,
            cursorColor = c.accent,
            errorCursorColor = c.danger,
            focusedLabelColor = c.accent,
        )
    }
}

@ThemePreviews
@Composable
fun SurauTextFieldPreview() {
    SurauTheme {
        SurauBackground(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            SurauTextField(
                value = "",
                onValueChange = {},
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
