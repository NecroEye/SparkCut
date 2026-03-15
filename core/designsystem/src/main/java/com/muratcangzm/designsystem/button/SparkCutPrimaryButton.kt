package com.muratcangzm.designsystem.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muratcangzm.designsystem.theme.SparkCutTheme

enum class SparkCutButtonSize {
    Medium,
    Large
}

@Composable
fun SparkCutPrimaryButton(
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

    Button(
        modifier = modifier.defaultMinSize(minHeight = minHeight),
        onClick = onClick,
        enabled = enabled && !loading,
        shape = shapes.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.background,
            disabledContainerColor = colors.primaryMuted.copy(alpha = 0.75f),
            disabledContentColor = colors.textMuted
        ),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.background) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                when {
                    loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = colors.background,
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