package com.muratcangzm.designsystem.banner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muratcangzm.designsystem.button.SparkCutSecondaryButton
import com.muratcangzm.designsystem.card.SparkCutCard
import com.muratcangzm.designsystem.card.SparkCutCardTone
import com.muratcangzm.designsystem.chip.SparkCutStatusChip
import com.muratcangzm.designsystem.chip.SparkCutStatusTone
import com.muratcangzm.designsystem.theme.SparkCutTheme

enum class SparkCutValidationBannerSeverity {
    Info,
    Warning,
    Error
}

@Composable
fun SparkCutValidationBanner(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    severity: SparkCutValidationBannerSeverity = SparkCutValidationBannerSeverity.Warning,
    issueCount: Int? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    leadingContent: (@Composable RowScope.() -> Unit)? = null
) {
    val colors = SparkCutTheme.colors
    val spacing = SparkCutTheme.spacing
    val typography = SparkCutTheme.typography

    val cardTone = when (severity) {
        SparkCutValidationBannerSeverity.Info -> SparkCutCardTone.Elevated
        SparkCutValidationBannerSeverity.Warning -> SparkCutCardTone.Default
        SparkCutValidationBannerSeverity.Error -> SparkCutCardTone.Focused
    }

    val statusTone = when (severity) {
        SparkCutValidationBannerSeverity.Info -> SparkCutStatusTone.Info
        SparkCutValidationBannerSeverity.Warning -> SparkCutStatusTone.Warning
        SparkCutValidationBannerSeverity.Error -> SparkCutStatusTone.Error
    }

    val chipText = buildString {
        append(
            when (severity) {
                SparkCutValidationBannerSeverity.Info -> "Info"
                SparkCutValidationBannerSeverity.Warning -> "Warning"
                SparkCutValidationBannerSeverity.Error -> "Error"
            }
        )

        if (issueCount != null && issueCount > 0) {
            append(" • ")
            append(issueCount)
            append(if (issueCount == 1) " issue" else " issues")
        }
    }

    SparkCutCard(
        modifier = modifier.fillMaxWidth(),
        tone = cardTone
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingContent != null) {
                    leadingContent()
                    Spacer(modifier = Modifier.width(spacing.sm))
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = typography.cardTitle,
                        color = colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = message,
                        style = typography.body,
                        color = colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.width(spacing.sm))

                SparkCutStatusChip(
                    text = chipText,
                    tone = statusTone
                )
            }

            if (!actionLabel.isNullOrBlank() && onActionClick != null) {
                SparkCutSecondaryButton(
                    text = actionLabel,
                    onClick = onActionClick
                )
            }
        }
    }
}