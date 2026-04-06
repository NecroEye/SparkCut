package com.muratcangzm.editor.ui.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.muratcangzm.designsystem.card.SparkCutCard
import com.muratcangzm.designsystem.card.SparkCutCardTone
import com.muratcangzm.designsystem.theme.SparkCutTheme
import com.muratcangzm.editor.ui.EditorContract
import com.muratcangzm.media.domain.MediaDurationFormatter
import kotlin.math.max

@Composable
fun EditorTimelineShell(
    state: EditorContract.State,
    selectedTab: EditorWorkspaceTab,
    selectedMediaIndex: Int,
    previewPositionMs: Long,
    onClipSelected: (Int) -> Unit,
    onTabSelected: (EditorWorkspaceTab) -> Unit,
) {
    val spacing = SparkCutTheme.spacing
    val typography = SparkCutTheme.typography
    val colors = SparkCutTheme.colors
    val shapes = SparkCutTheme.shapes
    val scrollState = rememberScrollState()

    val totalDurationMs = remember(state.selectedMedia) {
        state.selectedMedia.sumOf(::timelineClipDurationMs).coerceAtLeast(1L)
    }

    val mediaWidths = remember(state.selectedMedia) {
        state.selectedMedia.map(::timelineClipWidth)
    }

    val contentWidth = remember(key1 = mediaWidths) {
        val totalMediaWidth = mediaWidths.fold(0f) { acc, width -> acc + width.value }

        max(420f, totalMediaWidth + (state.selectedMedia.size * 14f) + 72f).dp + 72f.dp
    }

    val playheadFraction = (previewPositionMs.toFloat() / totalDurationMs.toFloat())
        .coerceIn(0f, 1f)

    SparkCutCard(
        modifier = Modifier.fillMaxWidth(),
        tone = SparkCutCardTone.Elevated,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "Timeline",
                        style = typography.sectionTitle,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "${state.selectedMedia.size} clips • ${MediaDurationFormatter.format(totalDurationMs)} total",
                        style = typography.meta,
                        color = colors.textMuted,
                    )
                }

                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .background(
                            color = colors.backgroundAlt,
                            shape = shapes.pill,
                        )
                        .clickable { },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = colors.textMuted,
                        )
                        Text(
                            text = "Tracks",
                            style = typography.body,
                            color = colors.textSecondary,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Column(
                    modifier = Modifier.width(58.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    TimelineLaneLabel(
                        label = "Text",
                        selected = selectedTab == EditorWorkspaceTab.Text,
                        onClick = { onTabSelected(EditorWorkspaceTab.Text) },
                    )
                    TimelineLaneLabel(
                        label = "FX",
                        selected = selectedTab == EditorWorkspaceTab.Transition,
                        onClick = { onTabSelected(EditorWorkspaceTab.Transition) },
                    )
                    TimelineLaneLabel(
                        label = "Media",
                        selected = selectedTab == EditorWorkspaceTab.Media,
                        onClick = { onTabSelected(EditorWorkspaceTab.Media) },
                    )
                }

                Box(
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .horizontalScroll(scrollState)
                    ) {
                        Box(
                            modifier = Modifier.width(contentWidth)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                            ) {
                                TimelineRuler(
                                    contentWidth = contentWidth,
                                    totalDurationMs = totalDurationMs,
                                    previewPositionMs = previewPositionMs,
                                )

                                TimelineTextLane(
                                    contentWidth = contentWidth,
                                    state = state,
                                )

                                TimelineFxLane(
                                    contentWidth = contentWidth,
                                    state = state,
                                )

                                TimelineMediaLane(
                                    items = state.selectedMedia,
                                    widths = mediaWidths,
                                    selectedIndex = selectedMediaIndex,
                                    onClipSelected = onClipSelected,
                                )
                            }

                            TimelinePlayhead(
                                contentWidth = contentWidth,
                                fraction = playheadFraction,
                                previewPositionMs = previewPositionMs,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineRuler(
    contentWidth: Dp,
    totalDurationMs: Long,
    previewPositionMs: Long,
) {
    val typography = SparkCutTheme.typography
    val colors = SparkCutTheme.colors

    Box(
        modifier = Modifier
            .width(contentWidth)
            .padding(horizontal = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "0:00",
                style = typography.meta,
                color = colors.textMuted,
            )
            Text(
                text = MediaDurationFormatter.format(previewPositionMs),
                style = typography.meta,
                color = colors.textPrimary,
            )
            Text(
                text = MediaDurationFormatter.format(totalDurationMs),
                style = typography.meta,
                color = colors.textMuted,
            )
        }
    }
}

@Composable
private fun TimelineTextLane(
    contentWidth: Dp,
    state: EditorContract.State,
) {
    val spacing = SparkCutTheme.spacing
    val colors = SparkCutTheme.colors
    val typography = SparkCutTheme.typography
    val shapes = SparkCutTheme.shapes

    Box(
        modifier = Modifier
            .width(contentWidth)
            .height(56.dp)
            .background(colors.backgroundAlt, shapes.lg),
    ) {
        if (state.textFields.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.textFields.take(2).forEach { field ->
                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .height(40.dp)
                            .background(colors.surfaceFocused, shapes.lg),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            text = if (field.value.isBlank()) field.label else field.value,
                            modifier = Modifier.padding(horizontal = 12.dp),
                            style = typography.body,
                            color = colors.textPrimary,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineFxLane(
    contentWidth: Dp,
    state: EditorContract.State,
) {
    val colors = SparkCutTheme.colors
    val typography = SparkCutTheme.typography
    val shapes = SparkCutTheme.shapes

    val selectedTransition = state.transitions.firstOrNull { it.isSelected }?.label

    Box(
        modifier = Modifier
            .width(contentWidth)
            .height(56.dp)
            .background(colors.backgroundAlt, shapes.lg),
    ) {
        if (selectedTransition != null) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                    .width(140.dp)
                    .height(36.dp)
                    .background(colors.surfaceFocused, shapes.pill),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = selectedTransition,
                    style = typography.meta,
                    color = colors.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun TimelineMediaLane(
    items: List<EditorContract.SelectedMediaItem>,
    widths: List<Dp>,
    selectedIndex: Int,
    onClipSelected: (Int) -> Unit,
) {
    val spacing = SparkCutTheme.spacing
    val colors = SparkCutTheme.colors
    val typography = SparkCutTheme.typography
    val shapes = SparkCutTheme.shapes

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items.forEachIndexed { index, item ->
            Box(
                modifier = Modifier
                    .width(widths.getOrElse(index) { 140.dp })
                    .height(88.dp)
                    .background(
                        color = if (selectedIndex == index) {
                            colors.surfaceFocused
                        } else {
                            colors.backgroundAlt
                        },
                        shape = shapes.xl,
                    )
                    .clickable { onClipSelected(index) }
                    .padding(12.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Clip ${index + 1}",
                        style = typography.meta,
                        color = colors.primary,
                    )

                    Text(
                        text = item.fileName,
                        style = typography.body,
                        color = colors.textPrimary,
                        maxLines = 2,
                    )

                    Text(
                        text = MediaDurationFormatter.format(timelineClipDurationMs(item)),
                        style = typography.meta,
                        color = colors.textMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelinePlayhead(
    contentWidth: Dp,
    fraction: Float,
    previewPositionMs: Long,
) {
    val colors = SparkCutTheme.colors
    val typography = SparkCutTheme.typography

    val xOffset = (contentWidth.value * fraction).dp

    Box(
        modifier = Modifier
            .offset(x = xOffset)
            .padding(top = 2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = colors.primary,
                        shape = RoundedCornerShape(999.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = MediaDurationFormatter.format(previewPositionMs),
                    style = typography.meta,
                    color = colors.textPrimary,
                )
            }

            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .width(2.dp)
                    .height(220.dp)
                    .background(colors.primary),
            )
        }
    }
}

@Composable
private fun TimelineLaneLabel(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = SparkCutTheme.colors
    val typography = SparkCutTheme.typography
    val shapes = SparkCutTheme.shapes

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                color = if (selected) {
                    colors.surfaceFocused
                } else {
                    colors.backgroundAlt
                },
                shape = shapes.lg,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = typography.meta,
            color = if (selected) colors.textPrimary else colors.textSecondary,
        )
    }
}

private fun timelineClipDurationMs(
    item: EditorContract.SelectedMediaItem,
): Long {
    return when {
        item.isVideo && item.trimStartMs != null && item.trimEndMs != null -> {
            (item.trimEndMs - item.trimStartMs).coerceAtLeast(300L)
        }

        item.isVideo && item.sourceDurationMs != null -> {
            item.sourceDurationMs.coerceAtLeast(300L)
        }

        else -> 2_500L
    }
}

private fun timelineClipWidth(
    item: EditorContract.SelectedMediaItem,
): Dp {
    val seconds = timelineClipDurationMs(item) / 1000f
    return (110f + (seconds * 26f)).coerceIn(140f, 240f).dp
}