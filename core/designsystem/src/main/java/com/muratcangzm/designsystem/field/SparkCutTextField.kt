package com.muratcangzm.designsystem.field

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muratcangzm.designsystem.theme.SparkCutTheme

@Composable
fun SparkCutTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val colors = SparkCutTheme.colors
    val shapes = SparkCutTheme.shapes
    val typography = SparkCutTheme.typography

    val isError = !errorText.isNullOrBlank()

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            isError = isError,
            shape = shapes.md,
            textStyle = typography.body.copy(color = colors.textPrimary),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            label = label?.let {
                {
                    Text(
                        text = it,
                        style = typography.meta
                    )
                }
            },
            placeholder = placeholder?.let {
                {
                    Text(
                        text = it,
                        style = typography.body,
                        color = colors.textMuted
                    )
                }
            },
            supportingText = {
                val resolvedText = errorText ?: supportingText
                if (!resolvedText.isNullOrBlank()) {
                    Text(
                        text = resolvedText,
                        style = typography.meta,
                        color = if (isError) colors.error else colors.textMuted
                    )
                }
            },
            leadingIcon = leadingContent,
            trailingIcon = trailingContent,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                disabledTextColor = colors.textMuted,
                focusedContainerColor = colors.surfaceFocused,
                unfocusedContainerColor = colors.surface,
                disabledContainerColor = colors.surface,
                errorContainerColor = colors.errorContainer.copy(alpha = 0.35f),

                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.strokeSoft,
                disabledBorderColor = colors.strokeSoft.copy(alpha = 0.55f),
                errorBorderColor = colors.error,

                focusedLabelColor = colors.primary,
                unfocusedLabelColor = colors.textMuted,
                disabledLabelColor = colors.textMuted,
                errorLabelColor = colors.error,

                focusedPlaceholderColor = colors.textMuted,
                unfocusedPlaceholderColor = colors.textMuted,
                disabledPlaceholderColor = colors.textMuted,

                focusedLeadingIconColor = colors.textSecondary,
                unfocusedLeadingIconColor = colors.textMuted,
                disabledLeadingIconColor = colors.textMuted,
                errorLeadingIconColor = colors.error,

                focusedTrailingIconColor = colors.textSecondary,
                unfocusedTrailingIconColor = colors.textMuted,
                disabledTrailingIconColor = colors.textMuted,
                errorTrailingIconColor = colors.error,

                focusedSupportingTextColor = colors.textMuted,
                unfocusedSupportingTextColor = colors.textMuted,
                disabledSupportingTextColor = colors.textMuted,
                errorSupportingTextColor = colors.error,

                cursorColor = colors.primary,
                errorCursorColor = colors.error
            )
        )
    }
}