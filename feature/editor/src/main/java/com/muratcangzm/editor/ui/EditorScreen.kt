package com.muratcangzm.editor.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muratcangzm.media.domain.MediaThumbnailProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.graphicsLayer
import com.muratcangzm.editor.ui.reorder.EditorReorderState
import com.muratcangzm.editor.ui.reorder.reorderDragHandle
import androidx.compose.material3.RangeSlider
import com.muratcangzm.media.domain.MediaDurationFormatter

@Composable
fun EditorScreen(
    templateId: String,
    mediaUris: List<String>,
    onBack: () -> Unit,
    onOpenExport: (EditorContract.ExportPayload) -> Unit,
    viewModel: EditorViewModel = koinViewModel(
        parameters = { parametersOf(templateId, mediaUris) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreenContent(
    state: EditorContract.State,
    snackbarHostState: SnackbarHostState,
    onEvent: (EditorContract.Event) -> Unit,
    thumbnailProvider: MediaThumbnailProvider,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editor") },
                navigationIcon = {
                    IconButton(
                        onClick = { onEvent(EditorContract.Event.BackClicked) }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        bottomBar = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = when {
                            state.isResolvingMedia -> "Reading media..."
                            state.canExport -> "Ready to continue"
                            else -> "Complete required fields to continue"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Button(
                        onClick = { onEvent(EditorContract.Event.ExportClicked) },
                        enabled = state.canExport,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MovieCreation,
                            contentDescription = null,
                        )
                        Text(
                            text = "Continue to export",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->

        val listState = rememberLazyListState()
        val reorderState = remember {
            EditorReorderState(
                listState = listState,
                onMove = { from, to ->
                    onEvent(EditorContract.Event.ReorderMedia(from, to))
                }
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                if (state.template != null) {
                    TemplateHeaderCard(template = state.template)
                } else {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = state.errorMessage ?: "Editor could not load template.",
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            item {
                EditorSectionCard(
                    icon = Icons.Outlined.AutoAwesome,
                    title = "Media sequence",
                    subtitle = if (state.isResolvingMedia) {
                        "Resolving media metadata..."
                    } else {
                        "Change the order to reassign assets to different template slots"
                    },
                ) {
                    if (state.selectedMedia.isEmpty()) {
                        Text(
                            text = "No media available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
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
                    icon = Icons.Outlined.TextFields,
                    title = "Text overlays",
                    subtitle = if (state.textFields.isEmpty()) {
                        "This template has no editable text"
                    } else {
                        "Update title, caption, CTA or other text fields"
                    },
                ) {
                    if (state.textFields.isEmpty()) {
                        Text(
                            text = "No editable text fields for this template.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            state.textFields.forEach { field ->
                                OutlinedTextField(
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
                                    label = {
                                        Text(
                                            text = if (field.required) {
                                                "${field.label} *"
                                            } else {
                                                field.label
                                            }
                                        )
                                    },
                                    placeholder = {
                                        Text(field.placeholder)
                                    },
                                    supportingText = {
                                        Text("${field.value.length}/${field.maxLength}")
                                    },
                                    singleLine = false,
                                )
                            }
                        }
                    }
                }
            }

            item {
                EditorSectionCard(
                    icon = Icons.Outlined.Tune,
                    title = "Transition preset",
                    subtitle = "Choose the overall transition style for this reel",
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.transitions) { item ->
                            FilterChip(
                                selected = item.isSelected,
                                onClick = {
                                    onEvent(
                                        EditorContract.Event.TransitionSelected(item.preset)
                                    )
                                },
                                label = { Text(item.label) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateHeaderCard(
    template: EditorContract.TemplateSummary,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = template.name,
                style = MaterialTheme.typography.titleLarge,
            )

            Text(
                text = template.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(template.categoryLabel) },
                )

                Text(
                    text = "Format ${template.aspectRatioLabel}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun EditorSectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EditorMediaThumbnailBox(
                uri = item.uri,
                isVideo = item.isVideo,
                thumbnailProvider = thumbnailProvider,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Item ${item.order + 1} • ${item.typeLabel}",
                    style = MaterialTheme.typography.titleSmall,
                )

                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(item.slotLabel) },
                )

                Text(
                    text = item.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                )

                item.resolutionLabel?.let { resolution ->
                    Text(
                        text = "Resolution: $resolution",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                item.durationLabel?.let { duration ->
                    Text(
                        text = "Duration: $duration",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                item.mimeType?.let { mimeType ->
                    Text(
                        text = mimeType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (item.canTrim && item.sourceDurationMs != null && item.trimStartMs != null && item.trimEndMs != null) {
                    val durationMs = item.sourceDurationMs.toFloat()
                    val currentRange = remember(item.trimStartMs, item.trimEndMs) {
                        androidx.compose.runtime.mutableStateOf(
                            item.trimStartMs.toFloat()..item.trimEndMs.toFloat()
                        )
                    }

                    Text(
                        text = "Trim: ${MediaDurationFormatter.format(item.trimStartMs)} - ${
                            MediaDurationFormatter.format(item.trimEndMs)
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    RangeSlider(
                        value = currentRange.value,
                        onValueChange = { currentRange.value = it },
                        onValueChangeFinished = {
                            onTrimChanged(
                                currentRange.value.start.toLong(),
                                currentRange.value.endInclusive.toLong(),
                            )
                        },
                        valueRange = 0f..durationMs,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
private fun EditorMediaThumbnailBox(
    uri: String,
    isVideo: Boolean,
    thumbnailProvider: MediaThumbnailProvider,
    modifier: Modifier = Modifier,
) {
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
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(18.dp),
            ),
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AssistChip(
            onClick = {},
            enabled = false,
            label = {
                Text(if (isVideo) "Video" else "Photo")
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp),
        )
    }
}