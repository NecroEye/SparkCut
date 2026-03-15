package com.muratcangzm.designsystem.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.muratcangzm.designsystem.theme.SparkCutTheme

enum class SparkCutCardTone {
    Default,
    Elevated,
    Focused,
    Accent
}

@Composable
fun SparkCutCard(
    modifier: Modifier = Modifier,
    tone: SparkCutCardTone = SparkCutCardTone.Default,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(all = SparkCutTheme.spacing.md),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = SparkCutTheme.colors
    val shapes = SparkCutTheme.shapes

    val containerColor = when (tone) {
        SparkCutCardTone.Default -> colors.surface
        SparkCutCardTone.Elevated -> colors.surfaceElevated
        SparkCutCardTone.Focused -> colors.surfaceFocused
        SparkCutCardTone.Accent -> colors.surfaceAccent
    }

    val borderColor = when (tone) {
        SparkCutCardTone.Default -> colors.strokeSoft.copy(alpha = 0.85f)
        SparkCutCardTone.Elevated -> colors.strokeSoft
        SparkCutCardTone.Focused -> colors.strokeStrong
        SparkCutCardTone.Accent -> colors.primary.copy(alpha = 0.45f)
    }

    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier.alpha(if (enabled) 1f else 0.65f),
        shape = shapes.lg,
        color = containerColor,
        contentColor = colors.textPrimary,
        border = BorderStroke(width = 1.dp, color = borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            enabled = enabled,
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(padding)
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.textPrimary) {
                content()
            }
        }
    }
}

@Composable
fun SparkCutSelectableCard(
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(all = SparkCutTheme.spacing.md),
    content: @Composable ColumnScope.() -> Unit
) {
    SparkCutCard(
        modifier = modifier,
        tone = if (selected) SparkCutCardTone.Focused else SparkCutCardTone.Default,
        enabled = enabled,
        onClick = onClick,
        padding = padding,
        content = content
    )
}