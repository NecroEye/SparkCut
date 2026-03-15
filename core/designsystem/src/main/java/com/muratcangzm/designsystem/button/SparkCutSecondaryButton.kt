package com.muratcangzm.designsystem.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muratcangzm.designsystem.theme.SparkCutTheme

@Composable
fun SparkCutSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    size: SparkCutButtonSize = SparkCutButtonSize.Large,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val colors = SparkCutTheme.colors
    val shapes = SparkCutTheme.shapes
    val spacing = SparkCutTheme.spacing
    val typography = SparkCutTheme.typography

    val minHeight = when (size) {
        SparkCutButtonSize.Medium -> 48.dp
        SparkCutButtonSize.Large -> 56.dp
    }

    OutlinedButton(
        modifier = modifier.defaultMinSize(minHeight = minHeight),
        onClick = onClick,
        enabled = enabled && !loading,
        shape = shapes.pill,
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) {
                colors.strokeStrong
            } else {
                colors.strokeSoft.copy(alpha = 0.55f)
            }
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.surfaceElevated,
            contentColor = colors.textPrimary,
            disabledContainerColor = colors.surface,
            disabledContentColor = colors.textMuted
        ),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.textPrimary) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                when {
                    loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = colors.textPrimary,
                            strokeWidth = 2.dp
                        )
                    }

                    leadingContent != null -> {
                        leadingContent()
                    }
                }

                Text(
                    text = text,
                    style = typography.button
                )

                if (!loading && trailingContent != null) {
                    trailingContent()
                }
            }
        }
    }
}