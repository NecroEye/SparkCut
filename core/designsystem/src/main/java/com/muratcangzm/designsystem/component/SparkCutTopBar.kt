package com.muratcangzm.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.muratcangzm.designsystem.theme.SparkCutTheme

@Composable
fun SparkCutTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    containerColor: Color = SparkCutTheme.colors.backgroundAlt,
    showBottomDivider: Boolean = true
) {
    val colors = SparkCutTheme.colors
    val spacing = SparkCutTheme.spacing
    val typography = SparkCutTheme.typography

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        contentColor = colors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(containerColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (subtitle == null) 64.dp else 76.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(spacing.md))

                if (navigationIcon != null) {
                    Box(contentAlignment = Alignment.Center) {
                        navigationIcon()
                    }
                    Spacer(modifier = Modifier.width(spacing.sm))
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        style = typography.sectionTitle,
                        color = colors.textPrimary,
                        maxLines = 1
                    )

                    if (!subtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = typography.meta,
                            color = colors.textMuted,
                            maxLines = 1
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    content = actions
                )

                Spacer(modifier = Modifier.width(spacing.md))
            }

            if (showBottomDivider) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.strokeSoft.copy(alpha = 0.7f))
                )
            }
        }
    }
}

@Composable
fun SparkCutTopBarTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = SparkCutTheme.colors
    val typography = SparkCutTheme.typography

    TextButton(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled
    ) {
        Text(
            text = text,
            style = typography.button,
            color = if (enabled) colors.primary else colors.textMuted
        )
    }
}

@Composable
fun SparkCutTopBarIconContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = SparkCutTheme.colors
    val shapes = SparkCutTheme.shapes

    Surface(
        modifier = modifier,
        color = colors.surfaceElevated,
        contentColor = colors.textPrimary,
        shape = shapes.pill
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.textPrimary) {
                content()
            }
        }
    }
}