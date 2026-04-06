package com.muratcangzm.editor.ui.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muratcangzm.designsystem.card.SparkCutCard
import com.muratcangzm.designsystem.card.SparkCutCardTone
import com.muratcangzm.designsystem.field.SparkCutTextField
import com.muratcangzm.designsystem.theme.SparkCutTheme
import com.muratcangzm.editor.ui.EditorContract
import com.muratcangzm.media.domain.MediaDurationFormatter
import com.muratcangzm.model.id.TextFieldId
import com.muratcangzm.model.template.TransitionPreset

@Composable
fun EditorInspectorPanel(
    state: EditorContract.State,
    selectedTab: EditorWorkspaceTab,
    selectedMediaIndex: Int,
    onTextChanged: (TextFieldId, String) -> Unit,
    onTransitionSelected: (TransitionPreset) -> Unit,
    onMoveMediaUp: (String) -> Unit,
    onMoveMediaDown: (String) -> Unit,
    onTrimChanged: (String, Long, Long) -> Unit,
) {
    when (selectedTab) {
        EditorWorkspaceTab.Media -> {
            MediaInspectorPanel(
                state = state,
                selectedMediaIndex = selectedMediaIndex,
                onMoveMediaUp = onMoveMediaUp,
                onMoveMediaDown = onMoveMediaDown,
                onTrimChanged = onTrimChanged,
            )
        }

        EditorWorkspaceTab.Text -> {
            TextInspectorPanel(
                state = state,
                onTextChanged = onTextChanged,
            )
        }

        EditorWorkspaceTab.Transition -> {
            TransitionInspectorPanel(
                state = state,
                onTransitionSelected = onTransitionSelected,
            )
        }
    }
}

@Composable
private fun MediaInspectorPanel(
    state: EditorContract.State,
    selectedMediaIndex: Int,
    onMoveMediaUp: (String) -> Unit,
    onMoveMediaDown: (String) -> Unit,
    onTrimChanged: (String, Long, Long) -> Unit,
) {
    val spacing = SparkCutTheme.spacing
    val typography = SparkCutTheme.typography
    val colors = SparkCutTheme.colors

    val item = state.selectedMedia.getOrNull(selectedMediaIndex)

    SparkCutCard(
        modifier = Modifier.fillMaxWidth(),
        tone = SparkCutCardTone.Elevated,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Text(
                text = "Media inspector",
                style = typography.sectionTitle,
                color = colors.textPrimary,
            )

            Text(
                text = "Inspect the selected clip, reorder it, or trim supported video.",
                style = typography.body,
                color = colors.textSecondary,
            )

            if (item == null) {
                Text(
                    text = "No media selected.",
                    style = typography.body,
                    color = colors.textMuted,
                )
                return@Column
            }

            EditorInspectorRow(
                label = "File",
                value = item.fileName,
            )
            EditorInspectorRow(
                label = "Type",
                value = if (item.isVideo) "Video" else "Photo",
            )
            EditorInspectorRow(
                label = "Mime",
                value = item.mimeType ?: "Unknown",
            )
            item.resolutionLabel?.let {
                EditorInspectorRow(
                    label = "Resolution",
                    value = it,
                )
            }
            item.durationLabel?.let {
                EditorInspectorRow(
                    label = "Duration",
                    value = it,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(
                    onClick = { onMoveMediaUp(item.uri) },
                    enabled = item.canMoveUp,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowUpward,
                        contentDescription = "Move up",
                    )
                }

                FilledTonalIconButton(
                    onClick = { onMoveMediaDown(item.uri) },
                    enabled = item.canMoveDown,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowDownward,
                        contentDescription = "Move down",
                    )
                }
            }

            if (
                item.canTrim &&
                item.sourceDurationMs != null &&
                item.trimStartMs != null &&
                item.trimEndMs != null
            ) {
                var currentStart by remember(item.uri, item.trimStartMs) {
                    mutableFloatStateOf(item.trimStartMs.toFloat())
                }
                var currentEnd by remember(item.uri, item.trimEndMs) {
                    mutableFloatStateOf(item.trimEndMs.toFloat())
                }

                SparkCutCard(
                    modifier = Modifier.fillMaxWidth(),
                    tone = SparkCutCardTone.Default,
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        Text(
                            text = "Trim range",
                            style = typography.cardTitle,
                            color = colors.textPrimary,
                        )

                        Text(
                            text = "${MediaDurationFormatter.format(currentStart.toLong())} • ${
                                MediaDurationFormatter.format(currentEnd.toLong())
                            }",
                            style = typography.meta,
                            color = colors.textMuted,
                        )

                        RangeSlider(
                            value = currentStart..currentEnd,
                            onValueChange = { range ->
                                currentStart = range.start
                                currentEnd = range.endInclusive
                            },
                            onValueChangeFinished = {
                                onTrimChanged(
                                    item.uri,
                                    currentStart.toLong(),
                                    currentEnd.toLong(),
                                )
                            },
                            valueRange = 0f..item.sourceDurationMs.toFloat(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TextInspectorPanel(
    state: EditorContract.State,
    onTextChanged: (TextFieldId, String) -> Unit,
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
            Text(
                text = "Text inspector",
                style = typography.sectionTitle,
                color = colors.textPrimary,
            )

            Text(
                text = "Edit title, caption, CTA, and other visible text layers.",
                style = typography.body,
                color = colors.textSecondary,
            )

            if (state.textFields.isEmpty()) {
                Text(
                    text = "This template has no editable text fields.",
                    style = typography.body,
                    color = colors.textMuted,
                )
            } else {
                state.textFields.forEach { field ->
                    SparkCutTextField(
                        value = field.value,
                        onValueChange = {
                            onTextChanged(field.id, it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = if (field.required) {
                            "${field.label} *"
                        } else {
                            field.label
                        },
                        placeholder = field.placeholder,
                        supportingText = "${field.value.length}/${field.maxLength}",
                        errorText = if (field.required && field.value.isBlank()) {
                            "Required field"
                        } else {
                            null
                        },
                        minLines = 2,
                        maxLines = 4,
                    )
                }
            }
        }
    }
}

@Composable
private fun TransitionInspectorPanel(
    state: EditorContract.State,
    onTransitionSelected: (TransitionPreset) -> Unit,
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
            Text(
                text = "Transition inspector",
                style = typography.sectionTitle,
                color = colors.textPrimary,
            )

            Text(
                text = "Choose the transition style applied across the sequence.",
                style = typography.body,
                color = colors.textSecondary,
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                state.transitions.forEach { item ->
                    FilterChip(
                        selected = item.isSelected,
                        onClick = { onTransitionSelected(item.preset) },
                        label = { Text(item.label) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorInspectorRow(
    label: String,
    value: String,
) {
    val colors = SparkCutTheme.colors
    val typography = SparkCutTheme.typography
    val shapes = SparkCutTheme.shapes

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(colors.backgroundAlt, shapes.lg)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = typography.body,
            color = colors.textSecondary,
        )

        Text(
            text = value,
            style = typography.body,
            color = colors.textPrimary,
        )
    }
}