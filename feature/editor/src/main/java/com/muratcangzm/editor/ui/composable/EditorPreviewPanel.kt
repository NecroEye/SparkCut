package com.muratcangzm.editor.ui.composable

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.muratcangzm.designsystem.card.SparkCutCard
import com.muratcangzm.designsystem.card.SparkCutCardTone
import com.muratcangzm.designsystem.chip.SparkCutStatusChip
import com.muratcangzm.designsystem.chip.SparkCutStatusTone
import com.muratcangzm.designsystem.theme.SparkCutTheme
import com.muratcangzm.editor.ui.EditorContract
import com.muratcangzm.media.domain.MediaDurationFormatter
import com.muratcangzm.media.domain.MediaThumbnailProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun EditorPreviewPanel(
    state: EditorContract.State,
    selectedMediaIndex: Int,
    previewPositionMs: Long,
    totalDurationMs: Long,
    thumbnailProvider: MediaThumbnailProvider,
    onClipSelected: (Int) -> Unit,
) {
    val spacing = SparkCutTheme.spacing
    val typography = SparkCutTheme.typography
    val colors = SparkCutTheme.colors
    val shapes = SparkCutTheme.shapes

    val selectedItem = state.selectedMedia.getOrNull(selectedMediaIndex)

    SparkCutCard(
        modifier = Modifier.fillMaxWidth(),
        tone = SparkCutCardTone.Accent,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                SparkCutStatusChip(
                    text = state.selectedAspectRatio,
                    tone = SparkCutStatusTone.Neutral,
                )

                SparkCutStatusChip(
                    text = if (selectedItem?.isVideo == true) "Video" else "Media",
                    tone = SparkCutStatusTone.Neutral,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(248.dp)
                    .clip(shapes.xl)
                    .background(colors.backgroundAlt),
                contentAlignment = Alignment.Center,
            ) {
                if (selectedItem != null) {
                    EditorPreviewThumbnail(
                        uri = selectedItem.uri,
                        isVideo = selectedItem.isVideo,
                        thumbnailProvider = thumbnailProvider,
                    )

                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(colors.backgroundAlt.copy(alpha = 0.82f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = colors.textPrimary,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp),
                    ) {
                        SparkCutCard(
                            tone = SparkCutCardTone.Default,
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = selectedItem.fileName,
                                    style = typography.cardTitle,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )

                                Text(
                                    text = buildString {
                                        append(if (selectedItem.isVideo) "Video" else "Photo")
                                        append(" • ")
                                        append(selectedItem.fileName)
                                    },
                                    style = typography.meta,
                                    color = colors.textMuted,
                                )
                            }
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Outlined.VideoLibrary,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(42.dp),
                    )
                }
            }

            EditorPreviewMiniTrack(
                items = state.selectedMedia,
                selectedIndex = selectedMediaIndex,
                onClipSelected = onClipSelected,
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.surfaceFocused),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(
                                fraction = (previewPositionMs.toFloat() / totalDurationMs.toFloat())
                                    .coerceIn(0f, 1f)
                            )
                            .height(4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.primary),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = MediaDurationFormatter.format(previewPositionMs),
                        style = typography.meta,
                        color = colors.textMuted,
                    )

                    Text(
                        text = MediaDurationFormatter.format(totalDurationMs),
                        style = typography.meta,
                        color = colors.textMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorPreviewMiniTrack(
    items: List<EditorContract.SelectedMediaItem>,
    selectedIndex: Int,
    onClipSelected: (Int) -> Unit,
) {
    val spacing = SparkCutTheme.spacing
    val colors = SparkCutTheme.colors
    val typography = SparkCutTheme.typography
    val shapes = SparkCutTheme.shapes

    if (items.isEmpty()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        items.forEachIndexed { index, item ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(shapes.lg)
                    .background(
                        if (selectedIndex == index) {
                            colors.surfaceFocused
                        } else {
                            colors.backgroundAlt
                        }
                    )
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = "Clip ${index + 1}",
                    style = typography.meta,
                    color = if (selectedIndex == index) {
                        colors.textPrimary
                    } else {
                        colors.textMuted
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EditorPreviewThumbnail(
    uri: String,
    isVideo: Boolean,
    thumbnailProvider: MediaThumbnailProvider,
) {
    val colors = SparkCutTheme.colors

    val thumbnail by produceState<Bitmap?>(
        initialValue = null,
        key1 = uri,
        key2 = isVideo,
    ) {
        value = withContext(Dispatchers.IO) {
            thumbnailProvider.loadThumbnail(
                uri = uri,
                sizePx = 720,
            )
        }
    }

    if (thumbnail != null) {
        Image(
            bitmap = thumbnail!!.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    } else {
        Icon(
            imageVector = Icons.Outlined.VideoLibrary,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(42.dp),
        )
    }
}