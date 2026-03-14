package com.muratcangzm.create.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

@Composable
fun CreateScreen(
    templateId: String,
    onBack: () -> Unit,
    onOpenEditor: (templateId: String, mediaUris: List<String>) -> Unit,
    viewModel: CreateViewModel = koinViewModel(parameters = { parametersOf(templateId) }),
    thumbnailProvider: MediaThumbnailProvider = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val pickerMaxItems = state.remainingSelectableCount.coerceAtLeast(1)
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = pickerMaxItems,
        ),
    ) { uris: List<Uri> ->
        viewModel.onEvent(
            CreateContract.Event.MediaPicked(
                uris = uris.map(Uri::toString),
            )
        )
    }

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CreateContract.Effect.OpenMediaPicker -> {
                    pickerLauncher.launch(
                        PickVisualMediaRequest(
                            mediaType = ActivityResultContracts.PickVisualMedia.ImageAndVideo
                        )
                    )
                }

                is CreateContract.Effect.NavigateEditor -> {
                    onOpenEditor(effect.templateId.value, effect.mediaUris)
                }

                CreateContract.Effect.NavigateBack -> onBack()

                is CreateContract.Effect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    CreateScreenContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
        thumbnailProvider = thumbnailProvider,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreenContent(
    state: CreateContract.State,
    snackbarHostState: SnackbarHostState,
    onEvent: (CreateContract.Event) -> Unit,
    thumbnailProvider: MediaThumbnailProvider,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create") },
                navigationIcon = {
                    IconButton(
                        onClick = { onEvent(CreateContract.Event.BackClicked) }
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
                        text = if (state.isResolvingMedia) {
                            "Reading media metadata..."
                        } else {
                            state.selectionSummary
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Button(
                        onClick = { onEvent(CreateContract.Event.ContinueClicked) },
                        enabled = state.canContinue,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MovieCreation,
                            contentDescription = null,
                        )
                        Text(
                            text = "Continue to editor",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                if (state.template != null) {
                    TemplateSummaryCard(template = state.template)
                } else {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = state.errorMessage ?: "Template could not be loaded.",
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Select media",
                            style = MaterialTheme.typography.titleMedium,
                        )

                        Text(
                            text = "This template needs at least ${state.minRequiredCount} items and supports up to ${state.maxAllowedCount}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        FilledTonalButton(
                            onClick = { onEvent(CreateContract.Event.AddMediaClicked) },
                            enabled = state.canAddMore && state.template != null,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AddPhotoAlternate,
                                contentDescription = null,
                            )
                            Text(
                                text = if (state.isResolvingMedia) {
                                    "Reading selected media..."
                                } else {
                                    "Add photos or videos"
                                },
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }

            if (state.selectedMedia.isEmpty()) {
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Collections,
                                contentDescription = null,
                            )
                            Text(
                                text = "No media selected yet",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "Pick photos or short clips to start building this reel.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            items(
                items = state.selectedMedia,
                key = { it.uri },
            ) { item ->
                SelectedMediaCard(
                    item = item,
                    thumbnailProvider = thumbnailProvider,
                    onRemove = {
                        onEvent(CreateContract.Event.RemoveMediaClicked(item.uri))
                    },
                )
            }
        }
    }
}

@Composable
private fun TemplateSummaryCard(
    template: CreateContract.TemplateSummary,
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
                    text = "Format ${template.aspectRatioLabel} • ${template.minMediaCount}-${template.maxMediaCount} assets",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SelectedMediaCard(
    item: CreateContract.SelectedMediaItem,
    thumbnailProvider: MediaThumbnailProvider,
    onRemove: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MediaThumbnailBox(
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

                FilledTonalButton(
                    onClick = onRemove,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                    )
                    Text(
                        text = "Remove",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaThumbnailBox(
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
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(18.dp),
                    ),
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