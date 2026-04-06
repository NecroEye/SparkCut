package com.muratcangzm.editor.ui.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muratcangzm.editor.ui.EditorContract

private object CaptionsPanelColors {
    val Surface = Color(0xFF1A1F2A)
    val ItemSurface = Color(0xFF222832)
    val ItemBorder = Color(0xFF2E3648)
    val Accent = Color(0xFF00D4AA)
    val TextPrimary = Color(0xFFF5F7FB)
    val TextSecondary = Color(0xFF8B95A5)
    val Divider = Color(0xFF2A3040)
}

@Composable
fun CaptionsPanel(
    visible: Boolean,
    onOptionSelected: (EditorContract.CaptionsOption) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(CaptionsPanelColors.Surface)
                .padding(top = 12.dp, bottom = 16.dp),
        ) {
            // Drawer handle
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.16f)),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Horizontal scrollable options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CaptionOptionItem(
                    icon = Icons.Outlined.Add,
                    label = "Add\ncaptions",
                    onClick = { onOptionSelected(EditorContract.CaptionsOption.AddCaptions) },
                )
                CaptionOptionItem(
                    icon = Icons.Outlined.AutoAwesome,
                    label = "Auto\ncaptions",
                    highlighted = true,
                    onClick = { onOptionSelected(EditorContract.CaptionsOption.AutoCaptions) },
                )
                CaptionOptionItem(
                    icon = Icons.Outlined.Style,
                    label = "Caption\ntemplates",
                    onClick = { onOptionSelected(EditorContract.CaptionsOption.CaptionTemplates) },
                )
                CaptionOptionItem(
                    icon = Icons.Outlined.MusicNote,
                    label = "Auto\nlyrics",
                    onClick = { onOptionSelected(EditorContract.CaptionsOption.AutoLyrics) },
                )
                CaptionOptionItem(
                    icon = Icons.Outlined.Download,
                    label = "Import\ncaptions",
                    onClick = { onOptionSelected(EditorContract.CaptionsOption.ImportCaptions) },
                )
            }
        }
    }
}

@Composable
private fun CaptionOptionItem(
    icon: ImageVector,
    label: String,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CaptionsPanelColors.ItemSurface)
            .border(
                width = if (highlighted) 1.5.dp else 1.dp,
                color = if (highlighted) CaptionsPanelColors.Accent else CaptionsPanelColors.ItemBorder,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (highlighted) CaptionsPanelColors.Accent.copy(alpha = 0.14f)
                    else Color.White.copy(alpha = 0.06f)
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlighted) CaptionsPanelColors.Accent else CaptionsPanelColors.TextPrimary,
                modifier = Modifier.size(20.dp),
            )
        }

        Text(
            text = label,
            color = if (highlighted) CaptionsPanelColors.TextPrimary else CaptionsPanelColors.TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal,
            lineHeight = 14.sp,
            maxLines = 2,
        )
    }
}
