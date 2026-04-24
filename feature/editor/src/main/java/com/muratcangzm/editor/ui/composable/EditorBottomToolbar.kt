package com.muratcangzm.editor.ui.composable

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muratcangzm.editor.ui.EditorContract

private object ToolbarColors {
    val Surface = Color(0xFF161B22)
    val Accent = Color(0xFF00D4AA)
    val AccentDim = Color(0xFF00D4AA).copy(alpha = 0.15f)
    val TextActive = Color(0xFFFFFFFF)
    val TextInactive = Color(0xFF8B95A5)
    val Border = Color(0xFF2A3040)
}

@Composable
fun EditorBottomToolbar(
    activeTab: EditorContract.ToolbarTab?,
    onTabSelected: (EditorContract.ToolbarTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ToolbarColors.Surface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        EditorContract.ToolbarTab.entries.forEach { tab ->
            ToolbarItem(
                icon = tab.icon(),
                label = tab.label(),
                isSelected = activeTab == tab,
                onClick = { onTabSelected(tab) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ToolbarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) ToolbarColors.Accent else ToolbarColors.TextInactive,
        animationSpec = tween(200),
        label = "iconColor",
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) ToolbarColors.TextActive else ToolbarColors.TextInactive,
        animationSpec = tween(200),
        label = "textColor",
    )

    val bottomBorder by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 0.dp,
        animationSpec = tween(200),
        label = "bottomBorder",
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .then(
                    if (isSelected) {
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ToolbarColors.AccentDim)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(22.dp),
            )
        }

        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(width = 20.dp, height = bottomBorder)
                    .clip(RoundedCornerShape(999.dp))
                    .background(ToolbarColors.Accent),
            )
        }
    }
}

private fun EditorContract.ToolbarTab.icon(): ImageVector = when (this) {
    EditorContract.ToolbarTab.Text -> Icons.Outlined.TextFields
    EditorContract.ToolbarTab.Captions -> Icons.Outlined.ClosedCaption
    EditorContract.ToolbarTab.AspectRatio -> Icons.Outlined.AspectRatio
}

private fun EditorContract.ToolbarTab.label(): String = when (this) {
    EditorContract.ToolbarTab.Text -> "Text"
    EditorContract.ToolbarTab.Captions -> "Captions"
    EditorContract.ToolbarTab.AspectRatio -> "Aspect ratio"
}
