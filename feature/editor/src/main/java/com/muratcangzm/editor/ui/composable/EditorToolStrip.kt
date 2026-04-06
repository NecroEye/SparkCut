package com.muratcangzm.editor.ui.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.muratcangzm.designsystem.card.SparkCutCard
import com.muratcangzm.designsystem.card.SparkCutCardTone
import com.muratcangzm.designsystem.theme.SparkCutTheme

enum class EditorWorkspaceTab {
    Media,
    Text,
    Transition,
}

@Composable
fun EditorToolStrip(
    selectedTab: EditorWorkspaceTab,
    onTabSelected: (EditorWorkspaceTab) -> Unit,
) {
    val spacing = SparkCutTheme.spacing
    val colors = SparkCutTheme.colors
    val typography = SparkCutTheme.typography
    val shapes = SparkCutTheme.shapes

    SparkCutCard(
        modifier = Modifier.fillMaxWidth(),
        tone = SparkCutCardTone.Elevated,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            EditorToolStripItem(
                modifier = Modifier.weight(1f),
                title = "Media",
                icon = Icons.Outlined.VideoLibrary,
                selected = selectedTab == EditorWorkspaceTab.Media,
                onClick = { onTabSelected(EditorWorkspaceTab.Media) },
            )

            EditorToolStripItem(
                modifier = Modifier.weight(1f),
                title = "Text",
                icon = Icons.Outlined.TextFields,
                selected = selectedTab == EditorWorkspaceTab.Text,
                onClick = { onTabSelected(EditorWorkspaceTab.Text) },
            )

            EditorToolStripItem(
                modifier = Modifier.weight(1f),
                title = "Transition",
                icon = Icons.Outlined.Tune,
                selected = selectedTab == EditorWorkspaceTab.Transition,
                onClick = { onTabSelected(EditorWorkspaceTab.Transition) },
            )
        }
    }
}

@Composable
private fun EditorToolStripItem(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SparkCutTheme.colors
    val typography = SparkCutTheme.typography
    val shapes = SparkCutTheme.shapes

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(shapes.pill)
            .background(
                if (selected) {
                    colors.surfaceFocused
                } else {
                    colors.backgroundAlt
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) colors.primary else colors.textMuted,
            )

            Text(
                text = title,
                style = typography.body,
                color = if (selected) colors.textPrimary else colors.textSecondary,
            )
        }
    }
}