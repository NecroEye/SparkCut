package com.muratcangzm.editor.ui.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muratcangzm.editor.ui.EditorContract

private object AutoCaptionsColors {
    val Surface = Color(0xFF1A1F2A)
    val SurfaceElevated = Color(0xFF222832)
    val Accent = Color(0xFF00D4AA)
    val AccentSecondary = Color(0xFF00E5C3)
    val ChipActive = Color(0xFF00D4AA)
    val ChipInactive = Color(0xFF2A3040)
    val ChipTextActive = Color(0xFF0A0F14)
    val TextPrimary = Color(0xFFF5F7FB)
    val TextSecondary = Color(0xFF8B95A5)
    val TextMuted = Color(0xFF6F7B8E)
    val Border = Color(0xFF2E3648)
    val Pro = Color(0xFF6366F1)
}

@Composable
fun AutoCaptionsSheet(
    visible: Boolean,
    config: EditorContract.AutoCaptionsConfig,
    onLanguageChanged: (String) -> Unit,
    onFillerWordsToggled: (Boolean) -> Unit,
    onSourceChanged: (EditorContract.CaptionSource) -> Unit,
    onGenerate: () -> Unit,
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
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(AutoCaptionsColors.Surface)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Generate auto captions",
                    color = AutoCaptionsColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = AutoCaptionsColors.TextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Language selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AutoCaptionsColors.SurfaceElevated)
                    .border(1.dp, AutoCaptionsColors.Border, RoundedCornerShape(14.dp))
                    .clickable { /* TODO: language picker */ }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Generate from",
                    color = AutoCaptionsColors.TextSecondary,
                    fontSize = 14.sp,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = config.language,
                    color = AutoCaptionsColors.Accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = AutoCaptionsColors.Accent,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Source selector chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AutoCaptionsColors.SurfaceElevated)
                    .border(1.dp, AutoCaptionsColors.Border, RoundedCornerShape(14.dp))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                EditorContract.CaptionSource.entries.forEach { source ->
                    val isSelected = config.source == source
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) AutoCaptionsColors.ChipActive else Color.Transparent,
                        animationSpec = tween(200),
                        label = "chipBg",
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) AutoCaptionsColors.ChipTextActive else AutoCaptionsColors.TextSecondary,
                        animationSpec = tween(200),
                        label = "chipText",
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor)
                            .clickable { onSourceChanged(source) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = source.label(),
                            color = textColor,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Identify filler words
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AutoCaptionsColors.SurfaceElevated)
                    .border(1.dp, AutoCaptionsColors.Border, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Identify filler words",
                            color = AutoCaptionsColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AutoCaptionsColors.Pro)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "Pro",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Text(
                        text = "Identify filler words to delete them in 1 tap.",
                        color = AutoCaptionsColors.TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                }

                Switch(
                    checked = config.identifyFillerWords,
                    onCheckedChange = onFillerWordsToggled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AutoCaptionsColors.Accent,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = AutoCaptionsColors.ChipInactive,
                        uncheckedBorderColor = Color.Transparent,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Progress indicator (visible during generation)
            if (config.isGenerating) {
                LinearProgressIndicator(
                    progress = { config.generationProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = AutoCaptionsColors.Accent,
                    trackColor = AutoCaptionsColors.Border,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Generate button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (config.isGenerating) {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    AutoCaptionsColors.Accent.copy(alpha = 0.5f),
                                    AutoCaptionsColors.AccentSecondary.copy(alpha = 0.5f),
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    AutoCaptionsColors.Accent,
                                    AutoCaptionsColors.AccentSecondary,
                                )
                            )
                        }
                    )
                    .clickable(enabled = !config.isGenerating, onClick = onGenerate),
                contentAlignment = Alignment.Center,
            ) {
                if (config.isGenerating) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFF0A0F14),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = "Generating...",
                            color = Color(0xFF0A0F14),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    Text(
                        text = "Generate",
                        color = Color(0xFF0A0F14),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

private fun EditorContract.CaptionSource.label(): String = when (this) {
    EditorContract.CaptionSource.All -> "All"
    EditorContract.CaptionSource.Voiceover -> "Voiceover"
    EditorContract.CaptionSource.Video -> "Video"
}
