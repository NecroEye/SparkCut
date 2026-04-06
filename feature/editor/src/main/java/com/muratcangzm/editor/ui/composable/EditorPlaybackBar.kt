package com.muratcangzm.editor.ui.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.muratcangzm.designsystem.card.SparkCutCard
import com.muratcangzm.designsystem.card.SparkCutCardTone
import com.muratcangzm.designsystem.theme.SparkCutTheme
import com.muratcangzm.media.domain.MediaDurationFormatter

@Composable
fun EditorPlaybackBar(
    positionMs: Long,
    totalDurationMs: Long,
    onPositionChanged: (Long) -> Unit,
) {
    val spacing = SparkCutTheme.spacing
    val typography = SparkCutTheme.typography
    val colors = SparkCutTheme.colors

    SparkCutCard(
        modifier = Modifier.fillMaxWidth(),
        tone = SparkCutCardTone.Elevated,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Slider(
                value = positionMs.toFloat().coerceAtMost(totalDurationMs.toFloat()),
                onValueChange = { onPositionChanged(it.toLong()) },
                valueRange = 0f..totalDurationMs.toFloat(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = MediaDurationFormatter.format(positionMs),
                    style = typography.meta,
                    color = colors.textMuted,
                )
                Text(
                    text = MediaDurationFormatter.format(totalDurationMs),
                    style = typography.meta,
                    color = colors.textMuted,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(
                    onClick = {
                        onPositionChanged((positionMs - 1_000L).coerceAtLeast(0L))
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FastRewind,
                        contentDescription = "Rewind",
                    )
                }

                FilledTonalIconButton(
                    onClick = {
                        onPositionChanged((positionMs + 500L).coerceAtMost(totalDurationMs))
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = "Play preview",
                    )
                }

                FilledTonalIconButton(
                    onClick = {
                        onPositionChanged((positionMs + 1_000L).coerceAtMost(totalDurationMs))
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FastForward,
                        contentDescription = "Forward",
                    )
                }
            }
        }
    }
}