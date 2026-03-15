package com.muratcangzm.designsystem.chip

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muratcangzm.designsystem.theme.SparkCutTheme

enum class SparkCutStatusTone {
    Neutral,
    Info,
    Success,
    Warning,
    Error,
    Accent
}

@Composable
fun SparkCutStatusChip(
    text: String,
    modifier: Modifier = Modifier,
    tone: SparkCutStatusTone = SparkCutStatusTone.Neutral,
    leadingContent: (@Composable RowScope.() -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = SparkCutTheme.spacing.sm,
        vertical = 6.dp
    )
) {
    val colors = SparkCutTheme.colors
    val shapes = SparkCutTheme.shapes
    val typography = SparkCutTheme.typography
    val spacing = SparkCutTheme.spacing

    val (containerColor, contentColor, borderColor) = when (tone) {
        SparkCutStatusTone.Neutral -> Triple(
            colors.surfaceElevated,
            colors.textSecondary,
            colors.strokeSoft
        )
        SparkCutStatusTone.Info -> Triple(
            colors.infoContainer,
            colors.info,
            colors.info.copy(alpha = 0.35f)
        )
        SparkCutStatusTone.Success -> Triple(
            colors.successContainer,
            colors.success,
            colors.success.copy(alpha = 0.35f)
        )
        SparkCutStatusTone.Warning -> Triple(
            colors.warningContainer,
            colors.warning,
            colors.warning.copy(alpha = 0.35f)
        )
        SparkCutStatusTone.Error -> Triple(
            colors.errorContainer,
            colors.error,
            colors.error.copy(alpha = 0.35f)
        )
        SparkCutStatusTone.Accent -> Triple(
            colors.primaryMuted,
            colors.primary,
            colors.primary.copy(alpha = 0.35f)
        )
    }

    Surface(
        modifier = modifier,
        shape = shapes.pill,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(width = 1.dp, color = borderColor)
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                modifier = Modifier.padding(contentPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                if (leadingContent != null) {
                    leadingContent()
                }

                Text(
                    text = text,
                    style = typography.label,
                    color = contentColor
                )

                if (trailingContent != null) {
                    trailingContent()
                }
            }
        }
    }
}