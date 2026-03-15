package com.muratcangzm.editor.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muratcangzm.designsystem.banner.SparkCutValidationBanner
import com.muratcangzm.designsystem.banner.SparkCutValidationBannerSeverity
import com.muratcangzm.designsystem.button.SparkCutButtonSize
import com.muratcangzm.designsystem.button.SparkCutPrimaryButton
import com.muratcangzm.designsystem.button.SparkCutSecondaryButton
import com.muratcangzm.designsystem.card.SparkCutCard
import com.muratcangzm.designsystem.card.SparkCutCardTone
import com.muratcangzm.designsystem.chip.SparkCutStatusChip
import com.muratcangzm.designsystem.chip.SparkCutStatusTone
import com.muratcangzm.designsystem.component.SparkCutScaffold
import com.muratcangzm.designsystem.component.SparkCutTopBar
import com.muratcangzm.designsystem.field.SparkCutTextField
import com.muratcangzm.designsystem.theme.SparkCutTheme
import com.muratcangzm.editor.ui.reorder.EditorReorderState
import com.muratcangzm.editor.ui.reorder.reorderDragHandle
import com.muratcangzm.media.domain.MediaDurationFormatter
import com.muratcangzm.media.domain.MediaThumbnailProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun EditorScreen(
    templateId: String,
    mediaUris: List<String>,
    onBack: () -> Unit,
    onOpenExport: (EditorContract.ExportPayload) -> Unit,
    viewModel: EditorViewModel = koinViewModel(
        parameters = { parametersOf(templateId, mediaUris, null) }
    ),
    thumbnailProvider: MediaThumbnailProvider = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                EditorContract.Effect.NavigateBack -> onBack()
                is EditorContract.Effect.NavigateExport -> onOpenExport(effect.payload)
                is EditorContract.Effect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    EditorScreenContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
        thumbnailProvider = thumbnailProvider,
    )
}

@Composable
private fun EditorScreenContent(
    state: EditorContract.State,
    snackbarHostState: SnackbarHostState,
    onEvent: (EditorContract.Event) -> Unit,
    thumbnailProvider: MediaThumbnailProvider,
) {
    val spacing = SparkCutTheme.spacing
    val colors = SparkCutTheme.colors

    val listState = rememberLazyListState()
    val reorderState = remember {
        EditorReorderState(
            listState = listState,
            onMove = { from, to ->
                onEvent(EditorContract.Event.ReorderMedia(from, to))
            }
        )
    }

    SparkCutScaffold(
        snackbarHostState = snackbarHostState,
        topBar = {
            SparkCutTopBar(
                title = "Editor",
                subtitle = state.template?.name ?: "Project editing",
                navigationIcon = {
                    IconButton(
                        onClick = { onEvent(EditorContract.Event.BackClicked) }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (!state.isLoading && state.template != null) {
                EditorBottomBar(
                    state = state,
                    onExportClick = {
                        onEvent(EditorContract.Event.ExportClicked)
                    }
                )
            }
        }
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = colors.primary
                    )
                }
            }

            state.template == null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    item {
                        SparkCutValidationBanner(
                            title = "Editor unavailable",
                            message = state.errorMessage ?: "The editor could not load this project.",
                            severity = SparkCutValidationBannerSeverity.Error
                        )
                    }

                    item {
                        SparkCutSecondaryButton(
                            text = "Go back",
                            onClick = { onEvent(EditorContract.Event.BackClicked) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    item {
                        EditorHeaderCard(
                            state = state
                        )
                    }

                    if (state.validationErrors.isNotEmpty() || state.validationWarnings.isNotEmpty()) {
                        item {
                            ValidationSummaryCard(
                                errors = state.validationErrors,
                                warnings = state.validationWarnings,
                            )
                        }
                    }

                    item {
                        EditorSectionCard(
                            title = "Media sequence",
                            subtitle = if (state.isResolvingMedia) {
                                "Resolving media metadata and preparing editable clips."
                            } else {
                                "Reorder items to change slot assignments and trim supported clips."
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = colors.textPrimary
                                )
                            }
                        ) {
                            if (state.selectedMedia.isEmpty()) {
                                Text(
                                    text = "No media available for this project.",
                                    style = SparkCutTheme.typography.body,
                                    color = colors.textSecondary
                                )
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(spacing.sm)
                                ) {
                                    state.selectedMedia.forEachIndexed { index, item ->
                                        EditorMediaCard(
                                            item = item,
                                            thumbnailProvider = thumbnailProvider,
                                            onMoveUp = {
                                                onEvent(EditorContract.Event.MoveMediaUp(item.uri))
                                            },
                                            onMoveDown = {
                                                onEvent(EditorContract.Event.MoveMediaDown(item.uri))
                                            },
                                            onTrimChanged = { startMs, endMs ->
                                                onEvent(
                                                    EditorContract.Event.TrimChanged(
                                                        uri = item.uri,
                                                        startMs = startMs,
                                                        endMs = endMs,
                                                    )
                                                )
                                            },
                                            modifier = Modifier
                                                .graphicsLayer {
                                                    translationY = reorderState.offsetFor(index)
                                                }
                                                .reorderDragHandle(
                                                    index = index,
                                                    state = reorderState,
                                                ),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        EditorSectionCard(
                            title = "Text overlays",
                            subtitle = if (state.textFields.isEmpty()) {
                                "This template does not expose editable text layers."
                            } else {
                                "Update titles, captions and CTA text before export."
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.TextFields,
                                    contentDescription = null,
                                    tint = colors.textPrimary
                                )
                            }
                        ) {
                            if (state.textFields.isEmpty()) {
                                Text(
                                    text = "No editable text fields for this template.",
                                    style = SparkCutTheme.typography.body,
                                    color = colors.textSecondary
                                )
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(spacing.sm)
                                ) {
                                    state.textFields.forEach { field ->
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(spacing.xs)
                                        ) {
                                            SparkCutTextField(
                                                value = field.value,
                                                onValueChange = {
                                                    onEvent(
                                                        EditorContract.Event.TextChanged(
                                                            fieldId = field.id,
                                                            value = it,
                                                        )
                                                    )
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
                                                minLines = 3,
                                                maxLines = 5
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        EditorSectionCard(
                            title = "Transition preset",
                            subtitle = "Choose the overall transition style used across the reel.",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Tune,
                                    contentDescription = null,
                                    tint = colors.textPrimary
                                )
                            }
                        ) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                            ) {
                                items(state.transitions) { item ->
                                    FilterChip(
                                        selected = item.isSelected,
                                        onClick = {
                                            onEvent(
                                                EditorContract.Event.TransitionSelected(item.preset)
                                            )
                                        },
                                        label = {
                                            Text(item.label)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        ExportReadinessCard(
                            state = state,
                            onExportClick = {
                                onEvent(EditorContract.Event.ExportClicked)
                            }
                        )
                    }

                    item {
                        Spacer(
                            modifier = Modifier.height(spacing.xxl)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorBottomBar(
    state: EditorContract.State,
    onExportClick: () -> Unit,
) {
    val spacing = SparkCutTheme.spacing
    val typography = SparkCutTheme.typography
    val colors = SparkCutTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.backgroundAlt)
            .navigationBarsPadding()
            .padding(
                horizontal = spacing.md,
                vertical = spacing.sm
            )
    ) {
        SparkCutCard(
            modifier = Modifier.fillMaxWidth(),
            tone = SparkCutCardTone.Elevated
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SparkCutStatusChip(
                        text = editorBottomBarStatusText(state),
                        tone = editorBottomBarStatusTone(state)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = editorAutosaveSecondaryText(state.autosaveState),
                        style = typography.meta,
                        color = if (state.autosaveState.status == EditorContract.AutosaveState.Status.Error) {
                            colors.error
                        } else {
                            colors.textMuted
                        }
                    )
                }

                Text(
                    text = editorBottomBarSummaryText(state),
                    style = typography.body,
                    color = colors.textSecondary
                )

                SparkCutPrimaryButton(
                    text = if (state.isResolvingMedia) {
                        "Preparing project..."
                    } else {
                        "Continue to export"
                    },
                    onClick = onExportClick,
                    enabled = state.canExport,
                    loading = state.isResolvingMedia,
                    size = SparkCutButtonSize.Large,
                    modifier = Modifier.fillMaxWidth(),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.MovieCreation,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun EditorHeaderCard(
    state: EditorContract.State,
) {
    val template = state.template ?: return
    val spacing = SparkCutTheme.spacing
    val typography = SparkCutTheme.typography
    val colors = SparkCutTheme.colors

    SparkCutCard(
        modifier = Modifier.fillMaxWidth(),
        tone = SparkCutCardTone.Accent
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                SparkCutStatusChip(
                    text = template.categoryLabel,
                    tone = SparkCutStatusTone.Info
                )
                SparkCutStatusChip(
                    text = template.aspectRatioLabel,
                    tone = SparkCutStatusTone.Neutral
                )
                SparkCutStatusChip(
                    text = if (state.canExport) "Ready" else "Editing",
                    tone = if (state.canExport) {
                        SparkCutStatusTone.Success
                    } else {
                        SparkCutStatusTone.Warning
                    }
                )
                SparkCutStatusChip(
                    text = editorAutosaveChipText(state.autosaveState),
                    tone = editorAutosaveChipTone(state.autosaveState)
                )
            }

            Text(
                text = template.name,
                style = typography.screenTitle,
                color = colors.textPrimary
            )

            Text(
                text = template.description,
                style = typography.body,
                color = colors.textSecondary
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                SparkCutStatusChip(
                    text = "${state.selectedMedia.size} media",
                    tone = SparkCutStatusTone.Neutral
                )
                SparkCutStatusChip(
                    text = "${state.textFields.size} text fields",
                    tone = SparkCutStatusTone.Neutral
                )
                SparkCutStatusChip(
                    text = "${state.transitions.size} transitions",
                    tone = SparkCutStatusTone.Neutral
                )
            }
        }
    }
}

@Composable
private fun ValidationSummaryCard(
    errors: List<String>,
    warnings: List<String>,
) {
    val spacing = SparkCutTheme.spacing
    val typography = SparkCutTheme.typography
    val colors = SparkCutTheme.colors

    val title = when {
        errors.isNotEmpty() -> "Validation issues found"
        warnings.isNotEmpty() -> "Review before export"
        else -> "Project validation"
    }

    val severity = when {
        errors.isNotEmpty() -> SparkCutValidationBannerSeverity.Error
        warnings.isNotEmpty() -> SparkCutValidationBannerSeverity.Warning
        else -> SparkCutValidationBannerSeverity.Info
    }

    val summaryMessage = buildString {
        if (errors.isNotEmpty()) {
            append("${errors.size} error")
            if (errors.size != 1) append("s")
        }
        if (warnings.isNotEmpty()) {
            if (isNotBlank()) append(" • ")
            append("${warnings.size} warning")
            if (warnings.size != 1) append("s")
        }
        if (isBlank()) {
            append("No issues detected.")
        }
    }

    SparkCutCard(
        modifier = Modifier.fillMaxWidth(),
        tone = when (severity) {
            SparkCutValidationBannerSeverity.Info -> SparkCutCardTone.Elevated
            SparkCutValidationBannerSeverity.Warning -> SparkCutCardTone.Default
            SparkCutValidationBannerSeverity.Error -> SparkCutCardTone.Focused
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            SparkCutValidationBanner(
                title = title,
                message = summaryMessage,
                severity = severity,
                issueCount = errors.size + warnings.size
            )

            if (errors.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    errors.forEach { message ->
                        Text(
                            text = "• $message",
                            style = typography.body,
                            color = colors.error
                        )
                    }
                }
            }

            if (warnings.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    warnings.forEach { message ->
                        Text(
                            text = "• $message",
                            style = typography.body,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorSectionCard(
    title: String,
    subtitle: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val spacing = SparkCutTheme.spacing
    val typography = SparkCutTheme.typography
    val colors = SparkCutTheme.colors

    SparkCutCard(
        modifier = Modifier.fillMaxWidth(),
        tone = SparkCutCardTone.Elevated
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            if (leadingIcon != null) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surfaceFocused),
                    contentAlignment = Alignment.Center
                ) {
                    leadingIcon()
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = typography.sectionTitle,
                    color = colors.textPrimary
                )
                Text(
                    text = subtitle,
                    style = typography.body,
                    color = colors.textSecondary
                )
            }

            content()
        }
    }
}

@Composable
private fun EditorMediaCard(
    item: EditorContract.SelectedMediaItem,
    thumbnailProvider: MediaThumbnailProvider,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onTrimChanged: (Long, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SparkCutTheme.spacing
    val typography = SparkCutTheme.typography
    val colors = SparkCutTheme.colors

    SparkCutCard(
        modifier = modifier.fillMaxWidth(),
        tone = SparkCutCardTone.Default
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorMediaThumbnailBox(
                uri = item.uri,
                isVideo = item.isVideo,
                thumbnailProvider = thumbnailProvider,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    SparkCutStatusChip(
                        text = "Item ${item.order + 1}",
                        tone = SparkCutStatusTone.Neutral
                    )
                    SparkCutStatusChip(
                        text = item.slotLabel,
                        tone = SparkCutStatusTone.Info
                    )
                    SparkCutStatusChip(
                        text = item.typeLabel,
                        tone = if (item.isVideo) {
                            SparkCutStatusTone.Info
                        } else {
                            SparkCutStatusTone.Success
                        }
                    )
                }

                Text(
                    text = item.fileName,
                    style = typography.cardTitle,
                    color = colors.textPrimary
                )

                item.resolutionLabel?.let { resolution ->
                    Text(
                        text = "Resolution: $resolution",
                        style = typography.meta,
                        color = colors.textMuted
                    )
                }

                item.durationLabel?.let { duration ->
                    Text(
                        text = "Duration: $duration",
                        style = typography.meta,
                        color = colors.textMuted
                    )
                }

                item.mimeType?.let { mimeType ->
                    Text(
                        text = mimeType,
                        style = typography.meta,
                        color = colors.textMuted
                    )
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

                    Text(
                        text = "Trim: ${MediaDurationFormatter.format(currentStart.toLong())} - ${
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
                                currentStart.toLong(),
                                currentEnd.toLong(),
                            )
                        },
                        valueRange = 0f..item.sourceDurationMs.toFloat(),
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    FilledTonalIconButton(
                        onClick = onMoveUp,
                        enabled = item.canMoveUp,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowUpward,
                            contentDescription = "Move up",
                        )
                    }

                    FilledTonalIconButton(
                        onClick = onMoveDown,
                        enabled = item.canMoveDown,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowDownward,
                            contentDescription = "Move down",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportReadinessCard(
    state: EditorContract.State,
    onExportClick: () -> Unit,
) {
    val canExport = state.canExport
    val severity = when {
        state.validationErrors.isNotEmpty() -> SparkCutValidationBannerSeverity.Error
        state.hasMissingRequiredFields || state.validationWarnings.isNotEmpty() -> SparkCutValidationBannerSeverity.Warning
        else -> SparkCutValidationBannerSeverity.Info
    }

    SparkCutValidationBanner(
        title = if (canExport) {
            "Ready for export"
        } else {
            "Export requirements not met"
        },
        message = when {
            state.isResolvingMedia -> "Media is still being prepared."
            state.validationErrors.isNotEmpty() -> "Resolve blocking issues before continuing."
            state.hasMissingRequiredFields -> "Complete all required text fields before export."
            state.validationWarnings.isNotEmpty() -> "You can continue after reviewing warnings."
            else -> "Project settings look good. Continue when ready."
        },
        severity = severity,
        issueCount = state.validationErrors.size + state.validationWarnings.size,
        actionLabel = if (canExport) "Continue to export" else null,
        onActionClick = if (canExport) onExportClick else null
    )
}

@Composable
private fun EditorMediaThumbnailBox(
    uri: String,
    isVideo: Boolean,
    thumbnailProvider: MediaThumbnailProvider,
    modifier: Modifier = Modifier,
) {
    val colors = SparkCutTheme.colors
    val shapes = SparkCutTheme.shapes

    val thumbnail by produceState<Bitmap?>(
        initialValue = null,
        key1 = uri,
        key2 = isVideo,
    ) {
        value = withContext(Dispatchers.IO) {
            thumbnailProvider.loadThumbnail(
                uri = uri,
                sizePx = 280,
            )
        }
    }

    Box(
        modifier = modifier
            .width(108.dp)
            .height(108.dp)
            .clip(shapes.lg)
            .background(colors.surfaceFocused),
        contentAlignment = Alignment.Center,
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = if (isVideo) {
                    Icons.Outlined.Movie
                } else {
                    Icons.Outlined.Image
                },
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = colors.textMuted,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(8.dp)
        ) {
            SparkCutStatusChip(
                text = if (isVideo) "Video" else "Photo",
                tone = if (isVideo) {
                    SparkCutStatusTone.Info
                } else {
                    SparkCutStatusTone.Success
                }
            )
        }
    }
}

private fun editorBottomBarStatusText(state: EditorContract.State): String {
    return when {
        state.isResolvingMedia -> "Processing"
        state.validationErrors.isNotEmpty() -> "Blocked"
        state.canExport -> "Ready"
        else -> "Needs review"
    }
}

private fun editorBottomBarStatusTone(state: EditorContract.State): SparkCutStatusTone {
    return when {
        state.isResolvingMedia -> SparkCutStatusTone.Info
        state.validationErrors.isNotEmpty() -> SparkCutStatusTone.Error
        state.canExport -> SparkCutStatusTone.Success
        else -> SparkCutStatusTone.Warning
    }
}

private fun editorAutosaveChipText(
    autosaveState: EditorContract.AutosaveState,
): String {
    return when (autosaveState.status) {
        EditorContract.AutosaveState.Status.Idle -> "Auto-save"
        EditorContract.AutosaveState.Status.Saving -> "Saving"
        EditorContract.AutosaveState.Status.Saved -> "Saved"
        EditorContract.AutosaveState.Status.Error -> "Save error"
    }
}

private fun editorAutosaveChipTone(
    autosaveState: EditorContract.AutosaveState,
): SparkCutStatusTone {
    return when (autosaveState.status) {
        EditorContract.AutosaveState.Status.Idle -> SparkCutStatusTone.Neutral
        EditorContract.AutosaveState.Status.Saving -> SparkCutStatusTone.Info
        EditorContract.AutosaveState.Status.Saved -> SparkCutStatusTone.Success
        EditorContract.AutosaveState.Status.Error -> SparkCutStatusTone.Error
    }
}

private fun editorAutosaveSecondaryText(
    autosaveState: EditorContract.AutosaveState,
): String {
    return autosaveState.message ?: when (autosaveState.status) {
        EditorContract.AutosaveState.Status.Idle -> "Auto-save is enabled"
        EditorContract.AutosaveState.Status.Saving -> "Saving changes..."
        EditorContract.AutosaveState.Status.Saved -> "All changes saved"
        EditorContract.AutosaveState.Status.Error -> "Project changes could not be saved."
    }
}

private fun editorBottomBarSummaryText(state: EditorContract.State): String {
    return when {
        state.isResolvingMedia -> {
            "SparkCut is resolving media and preparing the project."
        }

        state.validationErrors.isNotEmpty() -> {
            state.validationErrors.first()
        }

        state.hasMissingRequiredFields -> {
            "Complete required text fields before continuing."
        }

        state.validationWarnings.isNotEmpty() -> {
            state.validationWarnings.first()
        }

        state.canExport -> {
            "Project looks good. Continue to export when you are ready."
        }

        else -> {
            "Review the project configuration before export."
        }
    }
}