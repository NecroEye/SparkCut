package com.muratcangzm.editor.ui

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muratcangzm.designsystem.button.SparkCutButtonSize
import com.muratcangzm.designsystem.button.SparkCutPrimaryButton
import com.muratcangzm.designsystem.button.SparkCutSecondaryButton
import com.muratcangzm.designsystem.component.SparkCutScaffold
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

    val listState = rememberLazyListState()
    val reorderState = remember {
        EditorReorderState(
            listState = listState,
            onMove = { from, to ->
                onEvent(EditorContract.Event.ReorderMedia(from, to))
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SparkCutTheme.colors.background)
    ) {
        EditorBackgroundGlow()

        SparkCutScaffold(
            snackbarHostState = snackbarHostState,
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
                            color = SparkCutTheme.colors.primary
                        )
                    }
                }

                state.template == null -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(spacing.md),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        item {
                            EditorTopHeader(
                                autosaveState = state.autosaveState,
                                onBack = {
                                    onEvent(EditorContract.Event.BackClicked)
                                }
                            )
                        }

                        item {
                            SparkCutSecondaryButton(
                                text = "Go back",
                                onClick = {
                                    onEvent(EditorContract.Event.BackClicked)
                                },
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
                        contentPadding = PaddingValues(
                            start = spacing.md,
                            top = spacing.sm,
                            end = spacing.md,
                            bottom = spacing.xxl
                        ),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        item {
                            EditorTopHeader(
                                autosaveState = state.autosaveState,
                                onBack = {
                                    onEvent(EditorContract.Event.BackClicked)
                                }
                            )
                        }

                        item {
                            EditorHeroSection(state = state)
                        }

                        if (state.validationErrors.isNotEmpty() || state.validationWarnings.isNotEmpty()) {
                            item {
                                ValidationSpotlightCard(
                                    errors = state.validationErrors,
                                    warnings = state.validationWarnings
                                )
                            }
                        }

                        item {
                            EditorSectionHeader(
                                eyebrow = "SEQUENCE",
                                title = "Arrange your story",
                                subtitle = if (state.isResolvingMedia) {
                                    "Preparing media and metadata."
                                } else {
                                    "Drag, reorder and trim clips to shape the flow."
                                }
                            )
                        }

                        if (state.selectedMedia.isEmpty()) {
                            item {
                                EmptyEditorCard(
                                    title = "No media available",
                                    message = "There are no clips or photos connected to this project yet."
                                )
                            }
                        } else {
                            items(
                                count = state.selectedMedia.size,
                                key = { index -> state.selectedMedia[index].uri }
                            ) { index ->
                                val item = state.selectedMedia[index]
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

                        item {
                            EditorSectionHeader(
                                eyebrow = "TEXT",
                                title = "Overlay copy",
                                subtitle = if (state.textFields.isEmpty()) {
                                    "This template does not expose editable text."
                                } else {
                                    "Tune titles, captions and CTA copy before export."
                                }
                            )
                        }

                        item {
                            TextFieldsPanel(
                                state = state,
                                onEvent = onEvent
                            )
                        }

                        item {
                            EditorSectionHeader(
                                eyebrow = "TRANSITIONS",
                                title = "Motion language",
                                subtitle = "Pick how SparkCut moves from one clip to the next."
                            )
                        }

                        item {
                            TransitionPanel(
                                transitions = state.transitions,
                                onSelect = { preset ->
                                    onEvent(EditorContract.Event.TransitionSelected(preset))
                                }
                            )
                        }

                        item {
                            EditorSectionHeader(
                                eyebrow = "EXPORT",
                                title = "Final readiness",
                                subtitle = "Make sure the project is clean before moving forward."
                            )
                        }

                        item {
                            ExportReadinessPanel(
                                state = state,
                                onExportClick = {
                                    onEvent(EditorContract.Event.ExportClicked)
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.navigationBarsPadding())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorBackgroundGlow() {
    val colors = SparkCutTheme.colors

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = (-70).dp, y = (-20).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colors.primary.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.TopEnd)
                .offset(x = 55.dp, y = 110.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF8B5CF6).copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun EditorTopHeader(
    autosaveState: EditorContract.AutosaveState,
    onBack: () -> Unit,
) {
    val colors = SparkCutTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(14.dp),
            color = colors.surfaceElevated,
            border = BorderStroke(
                width = 1.dp,
                color = colors.strokeSoft.copy(alpha = 0.85f)
            )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.clickable(onClick = onBack)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Editor",
                style = SparkCutTheme.typography.sectionTitle,
                color = colors.textPrimary
            )
            Text(
                text = "Refine, reorder and prepare",
                style = SparkCutTheme.typography.meta,
                color = colors.textMuted
            )
        }

        MinimalEditorChip(
            text = editorAutosaveChipText(autosaveState),
            textColor = when (autosaveState.status) {
                EditorContract.AutosaveState.Status.Idle -> colors.textSecondary
                EditorContract.AutosaveState.Status.Saving -> colors.primary
                EditorContract.AutosaveState.Status.Saved -> Color(0xFF69E3A7)
                EditorContract.AutosaveState.Status.Error -> Color(0xFFFF8A8A)
            },
            containerColor = when (autosaveState.status) {
                EditorContract.AutosaveState.Status.Idle -> colors.surfaceElevated
                EditorContract.AutosaveState.Status.Saving -> colors.primaryMuted
                EditorContract.AutosaveState.Status.Saved -> Color(0xFF173126)
                EditorContract.AutosaveState.Status.Error -> Color(0xFF31191C)
            },
            borderColor = when (autosaveState.status) {
                EditorContract.AutosaveState.Status.Idle -> colors.strokeSoft
                EditorContract.AutosaveState.Status.Saving -> colors.primary.copy(alpha = 0.24f)
                EditorContract.AutosaveState.Status.Saved -> Color(0xFF69E3A7).copy(alpha = 0.22f)
                EditorContract.AutosaveState.Status.Error -> Color(0xFFFF8A8A).copy(alpha = 0.22f)
            }
        )
    }
}

@Composable
private fun EditorHeroSection(
    state: EditorContract.State,
) {
    val template = state.template ?: return
    val colors = SparkCutTheme.colors

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            color = colors.strokeStrong.copy(alpha = 0.65f)
        )
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            colors.surfaceFocused,
                            Color(0xFF171D31),
                            Color(0xFF101722)
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(140.dp)
                    .offset(x = 26.dp, y = (-18).dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.primary.copy(alpha = 0.18f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroChip(template.categoryLabel)
                    HeroChip(template.aspectRatioLabel)
                    HeroChip(if (state.canExport) "Ready" else "Editing")
                }

                Text(
                    text = template.name,
                    style = SparkCutTheme.typography.display.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = colors.textPrimary
                )

                Text(
                    text = template.description,
                    style = SparkCutTheme.typography.body,
                    color = colors.textSecondary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatChip("${state.selectedMedia.size} media")
                    StatChip("${state.textFields.size} text fields")
                    StatChip("${state.transitions.size} transitions")
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.surface.copy(alpha = 0.82f),
                    border = BorderStroke(
                        width = 1.dp,
                        color = colors.strokeSoft.copy(alpha = 0.85f)
                    )
                ) {
                    Text(
                        text = editorAutosaveSecondaryText(state.autosaveState),
                        modifier = Modifier.padding(
                            horizontal = 14.dp,
                            vertical = 10.dp
                        ),
                        style = SparkCutTheme.typography.meta,
                        color = if (state.autosaveState.status == EditorContract.AutosaveState.Status.Error) {
                            Color(0xFFFF8A8A)
                        } else {
                            colors.textSecondary
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ValidationSpotlightCard(
    errors: List<String>,
    warnings: List<String>,
) {
    val colors = SparkCutTheme.colors

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = colors.surface,
        border = BorderStroke(
            width = 1.dp,
            color = when {
                errors.isNotEmpty() -> Color(0xFFFF8A8A).copy(alpha = 0.22f)
                warnings.isNotEmpty() -> Color(0xFFFFC46B).copy(alpha = 0.22f)
                else -> colors.strokeSoft
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = when {
                    errors.isNotEmpty() -> "Validation issues found"
                    warnings.isNotEmpty() -> "Review before export"
                    else -> "Validation"
                },
                style = SparkCutTheme.typography.sectionTitle,
                color = colors.textPrimary
            )

            if (errors.isNotEmpty()) {
                errors.forEach { message ->
                    IssueLine(
                        message = message,
                        accent = Color(0xFFFF8A8A)
                    )
                }
            }

            if (warnings.isNotEmpty()) {
                warnings.forEach { message ->
                    IssueLine(
                        message = message,
                        accent = Color(0xFFFFC46B)
                    )
                }
            }
        }
    }
}

@Composable
private fun IssueLine(
    message: String,
    accent: Color,
) {
    val colors = SparkCutTheme.colors

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(accent)
        )

        Text(
            text = message,
            style = SparkCutTheme.typography.body,
            color = colors.textSecondary
        )
    }
}

@Composable
private fun EditorSectionHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
) {
    val colors = SparkCutTheme.colors

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = eyebrow,
            style = SparkCutTheme.typography.label,
            color = colors.primary
        )
        Text(
            text = title,
            style = SparkCutTheme.typography.sectionTitle,
            color = colors.textPrimary
        )
        Text(
            text = subtitle,
            style = SparkCutTheme.typography.body,
            color = colors.textSecondary
        )
    }
}

@Composable
private fun EmptyEditorCard(
    title: String,
    message: String,
) {
    val colors = SparkCutTheme.colors

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = colors.surface,
        border = BorderStroke(
            width = 1.dp,
            color = colors.strokeSoft.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = title,
                style = SparkCutTheme.typography.sectionTitle,
                color = colors.textPrimary
            )

            Text(
                text = message,
                style = SparkCutTheme.typography.body,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun TextFieldsPanel(
    state: EditorContract.State,
    onEvent: (EditorContract.Event) -> Unit,
) {
    val colors = SparkCutTheme.colors
    val spacing = SparkCutTheme.spacing

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = colors.surface,
        border = BorderStroke(
            width = 1.dp,
            color = colors.strokeSoft.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            if (state.textFields.isEmpty()) {
                Text(
                    text = "No editable text fields for this template.",
                    style = SparkCutTheme.typography.body,
                    color = colors.textSecondary
                )
            } else {
                state.textFields.forEach { field ->
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

@Composable
private fun TransitionPanel(
    transitions: List<EditorContract.TransitionItem>,
    onSelect: (com.muratcangzm.model.template.TransitionPreset) -> Unit,
) {
    val colors = SparkCutTheme.colors

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = colors.surface,
        border = BorderStroke(
            width = 1.dp,
            color = colors.strokeSoft.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = transitions,
                    key = { it.preset.name }
                ) { item ->
                    TransitionChoicePill(
                        label = item.label,
                        selected = item.isSelected,
                        onClick = {
                            onSelect(item.preset)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransitionChoicePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = SparkCutTheme.colors

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            colors.primaryMuted
        } else {
            colors.surfaceElevated
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                colors.primary.copy(alpha = 0.28f)
            } else {
                colors.strokeSoft.copy(alpha = 0.85f)
            }
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 11.dp
            ),
            style = SparkCutTheme.typography.bodyStrong,
            color = if (selected) colors.primary else colors.textSecondary
        )
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
    val colors = SparkCutTheme.colors
    val spacing = SparkCutTheme.spacing

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = colors.surface,
        border = BorderStroke(
            width = 1.dp,
            color = colors.strokeSoft.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MinimalEditorChip(
                            text = "Item ${item.order + 1}",
                            textColor = colors.textSecondary,
                            containerColor = colors.surfaceElevated,
                            borderColor = colors.strokeSoft
                        )
                        MinimalEditorChip(
                            text = item.slotLabel,
                            textColor = colors.primary,
                            containerColor = colors.primaryMuted,
                            borderColor = colors.primary.copy(alpha = 0.20f)
                        )
                        MinimalEditorChip(
                            text = item.typeLabel,
                            textColor = if (item.isVideo) Color(0xFF6FD3FF) else Color(0xFF69E3A7),
                            containerColor = if (item.isVideo) Color(0xFF112533) else Color(0xFF173126),
                            borderColor = if (item.isVideo) Color(0xFF6FD3FF).copy(alpha = 0.18f) else Color(0xFF69E3A7).copy(alpha = 0.18f)
                        )
                    }

                    Text(
                        text = item.fileName,
                        style = SparkCutTheme.typography.sectionTitle,
                        color = colors.textPrimary
                    )

                    item.resolutionLabel?.let { resolution ->
                        Text(
                            text = "Resolution: $resolution",
                            style = SparkCutTheme.typography.meta,
                            color = colors.textMuted
                        )
                    }

                    item.durationLabel?.let { duration ->
                        Text(
                            text = "Duration: $duration",
                            style = SparkCutTheme.typography.meta,
                            color = colors.textMuted
                        )
                    }

                    item.mimeType?.let { mimeType ->
                        Text(
                            text = mimeType,
                            style = SparkCutTheme.typography.meta,
                            color = colors.textMuted
                        )
                    }
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

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surfaceElevated,
                    border = BorderStroke(
                        width = 1.dp,
                        color = colors.strokeSoft.copy(alpha = 0.8f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Trim window",
                            style = SparkCutTheme.typography.cardTitle,
                            color = colors.textPrimary
                        )

                        Text(
                            text = "${MediaDurationFormatter.format(currentStart.toLong())} - ${
                                MediaDurationFormatter.format(currentEnd.toLong())
                            }",
                            style = SparkCutTheme.typography.meta,
                            color = colors.textMuted
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
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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

@Composable
private fun ExportReadinessPanel(
    state: EditorContract.State,
    onExportClick: () -> Unit,
) {
    val colors = SparkCutTheme.colors

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = colors.surface,
        border = BorderStroke(
            width = 1.dp,
            color = when {
                state.validationErrors.isNotEmpty() -> Color(0xFFFF8A8A).copy(alpha = 0.22f)
                state.hasMissingRequiredFields || state.validationWarnings.isNotEmpty() -> Color(0xFFFFC46B).copy(alpha = 0.22f)
                else -> colors.primary.copy(alpha = 0.20f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroChip(if (state.canExport) "Ready" else "Needs review")
                HeroChip(if (state.validationErrors.isNotEmpty()) "Blocked" else "Export flow")
            }

            Text(
                text = if (state.canExport) {
                    "Ready for export"
                } else {
                    "Not ready yet"
                },
                style = SparkCutTheme.typography.sectionTitle,
                color = colors.textPrimary
            )

            Text(
                text = when {
                    state.isResolvingMedia -> "Media is still being prepared."
                    state.validationErrors.isNotEmpty() -> "Resolve blocking issues before continuing."
                    state.hasMissingRequiredFields -> "Complete all required text fields before export."
                    state.validationWarnings.isNotEmpty() -> "Review warnings before moving forward."
                    else -> "Project settings look clean and ready for output."
                },
                style = SparkCutTheme.typography.body,
                color = colors.textSecondary
            )

            if (state.canExport) {
                Button(
                    onClick = onExportClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.background
                    ),
                    contentPadding = PaddingValues(
                        horizontal = 18.dp,
                        vertical = 16.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MovieCreation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Continue to export",
                        style = SparkCutTheme.typography.button
                    )
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
    val colors = SparkCutTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = colors.surface.copy(alpha = 0.96f),
            border = BorderStroke(
                width = 1.dp,
                color = colors.strokeSoft.copy(alpha = 0.85f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MinimalEditorChip(
                        text = editorBottomBarStatusText(state),
                        textColor = when {
                            state.validationErrors.isNotEmpty() -> Color(0xFFFF8A8A)
                            state.canExport -> Color(0xFF69E3A7)
                            else -> SparkCutTheme.colors.primary
                        },
                        containerColor = when {
                            state.validationErrors.isNotEmpty() -> Color(0xFF31191C)
                            state.canExport -> Color(0xFF173126)
                            else -> colors.primaryMuted
                        },
                        borderColor = when {
                            state.validationErrors.isNotEmpty() -> Color(0xFFFF8A8A).copy(alpha = 0.18f)
                            state.canExport -> Color(0xFF69E3A7).copy(alpha = 0.18f)
                            else -> colors.primary.copy(alpha = 0.20f)
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = editorAutosaveSecondaryText(state.autosaveState),
                        style = SparkCutTheme.typography.meta,
                        color = if (state.autosaveState.status == EditorContract.AutosaveState.Status.Error) {
                            Color(0xFFFF8A8A)
                        } else {
                            colors.textMuted
                        }
                    )
                }

                Text(
                    text = editorBottomBarSummaryText(state),
                    style = SparkCutTheme.typography.body,
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
private fun EditorMediaThumbnailBox(
    uri: String,
    isVideo: Boolean,
    thumbnailProvider: MediaThumbnailProvider,
    modifier: Modifier = Modifier,
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
                sizePx = 280,
            )
        }
    }

    Box(
        modifier = modifier
            .width(112.dp)
            .height(112.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surfaceFocused),
        contentAlignment = Alignment.Center,
    ) {
        if (thumbnail != null) {
            androidx.compose.foundation.Image(
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
            MinimalEditorChip(
                text = if (isVideo) "Video" else "Photo",
                textColor = if (isVideo) Color(0xFF6FD3FF) else Color(0xFF69E3A7),
                containerColor = if (isVideo) Color(0xFF112533) else Color(0xFF173126),
                borderColor = if (isVideo) Color(0xFF6FD3FF).copy(alpha = 0.18f) else Color(0xFF69E3A7).copy(alpha = 0.18f)
            )
        }
    }
}

@Composable
private fun HeroChip(text: String) {
    val colors = SparkCutTheme.colors

    MinimalEditorChip(
        text = text,
        textColor = colors.textSecondary,
        containerColor = colors.surface.copy(alpha = 0.80f),
        borderColor = colors.strokeSoft.copy(alpha = 0.80f)
    )
}

@Composable
private fun StatChip(text: String) {
    val colors = SparkCutTheme.colors

    MinimalEditorChip(
        text = text,
        textColor = colors.textPrimary,
        containerColor = colors.surfaceElevated.copy(alpha = 0.95f),
        borderColor = colors.strokeStrong.copy(alpha = 0.65f)
    )
}

@Composable
private fun MinimalEditorChip(
    text: String,
    textColor: Color,
    containerColor: Color,
    borderColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(
            width = 1.dp,
            color = borderColor
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 8.dp
            ),
            style = SparkCutTheme.typography.label,
            color = textColor
        )
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
            "Project looks clean. Move forward when you are ready."
        }

        else -> {
            "Review the current setup before export."
        }
    }
}