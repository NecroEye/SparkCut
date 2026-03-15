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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
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
import com.muratcangzm.designsystem.theme.SparkCutTheme
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

@Composable
private fun CreateScreenContent(
    state: CreateContract.State,
    snackbarHostState: SnackbarHostState,
    onEvent: (CreateContract.Event) -> Unit,
    thumbnailProvider: MediaThumbnailProvider,
) {
    val spacing = SparkCutTheme.spacing
    val colors = SparkCutTheme.colors

    SparkCutScaffold(
        snackbarHostState = snackbarHostState,
        topBar = {
            SparkCutTopBar(
                title = "Create Project",
                subtitle = state.template?.name ?: "Set up your project",
                navigationIcon = {
                    IconButton(
                        onClick = { onEvent(CreateContract.Event.BackClicked) }
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
                CreateBottomBar(
                    state = state,
                    onContinueClick = {
                        onEvent(CreateContract.Event.ContinueClicked)
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
                            title = "Template unavailable",
                            message = state.errorMessage ?: "The selected template could not be loaded.",
                            severity = SparkCutValidationBannerSeverity.Error
                        )
                    }

                    item {
                        SparkCutSecondaryButton(
                            text = "Go back",
                            onClick = { onEvent(CreateContract.Event.BackClicked) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    item {
                        CreateTemplateHeroCard(
                            template = state.template
                        )
                    }

                    item {
                        CreateSelectionCard(
                            state = state,
                            onAddMediaClick = {
                                onEvent(CreateContract.Event.AddMediaClicked)
                            }
                        )
                    }

                    if (state.isResolvingMedia) {
                        item {
                            SparkCutValidationBanner(
                                title = "Reading selected media",
                                message = "SparkCut is resolving metadata for the selected items.",
                                severity = SparkCutValidationBannerSeverity.Info
                            )
                        }
                    } else if (!state.canContinue) {
                        item {
                            val requiredCount =
                                (state.minRequiredCount - state.selectedCount).coerceAtLeast(0)

                            SparkCutValidationBanner(
                                title = "More media required",
                                message = if (requiredCount > 0) {
                                    "Add $requiredCount more item${if (requiredCount == 1) "" else "s"} to continue to the editor."
                                } else {
                                    "Complete the required media selection to continue."
                                },
                                severity = SparkCutValidationBannerSeverity.Warning,
                                actionLabel = if (state.canAddMore) "Add media" else null,
                                onActionClick = if (state.canAddMore) {
                                    { onEvent(CreateContract.Event.AddMediaClicked) }
                                } else {
                                    null
                                }
                            )
                        }
                    } else {
                        item {
                            SparkCutValidationBanner(
                                title = "Ready to continue",
                                message = if (state.canAddMore) {
                                    "You can continue now or add a few more items before opening the editor."
                                } else {
                                    "Your media selection is complete and ready for editing."
                                },
                                severity = SparkCutValidationBannerSeverity.Info
                            )
                        }
                    }

                    if (state.selectedMedia.isEmpty()) {
                        item {
                            EmptyMediaStateCard()
                        }
                    } else {
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
private fun CreateBottomBar(
    state: CreateContract.State,
    onContinueClick: () -> Unit,
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
                        text = bottomBarStatusText(state),
                        tone = bottomBarStatusTone(state)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = state.selectionSummary,
                        style = typography.meta,
                        color = colors.textMuted
                    )
                }

                Text(
                    text = bottomBarSummaryText(state),
                    style = typography.body,
                    color = colors.textSecondary
                )

                SparkCutPrimaryButton(
                    text = if (state.isResolvingMedia) {
                        "Preparing media..."
                    } else {
                        "Continue to editor"
                    },
                    onClick = onContinueClick,
                    enabled = state.canContinue,
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
private fun CreateTemplateHeroCard(
    template: CreateContract.TemplateSummary,
) {
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
            SparkCutStatusChip(
                text = "Template selected",
                tone = SparkCutStatusTone.Accent
            )

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
                    text = template.categoryLabel,
                    tone = SparkCutStatusTone.Info
                )
                SparkCutStatusChip(
                    text = template.aspectRatioLabel,
                    tone = SparkCutStatusTone.Neutral
                )
            }

            Text(
                text = "Requires ${template.minMediaCount}-${template.maxMediaCount} assets",
                style = typography.meta,
                color = colors.textMuted
            )
        }
    }
}

@Composable
private fun CreateSelectionCard(
    state: CreateContract.State,
    onAddMediaClick: () -> Unit,
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
            Text(
                text = "Select media",
                style = typography.sectionTitle,
                color = colors.textPrimary
            )

            Text(
                text = "This template needs at least ${state.minRequiredCount} item${if (state.minRequiredCount == 1) "" else "s"} and supports up to ${state.maxAllowedCount}.",
                style = typography.body,
                color = colors.textSecondary
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                SparkCutStatusChip(
                    text = "Selected ${state.selectedCount}/${state.maxAllowedCount}",
                    tone = SparkCutStatusTone.Neutral
                )
                SparkCutStatusChip(
                    text = "Min ${state.minRequiredCount}",
                    tone = if (state.selectedCount >= state.minRequiredCount) {
                        SparkCutStatusTone.Success
                    } else {
                        SparkCutStatusTone.Warning
                    }
                )
            }

            SparkCutSecondaryButton(
                text = if (state.isResolvingMedia) {
                    "Reading selected media..."
                } else {
                    "Add photos or videos"
                },
                onClick = onAddMediaClick,
                enabled = state.canAddMore && state.template != null,
                modifier = Modifier.fillMaxWidth(),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun EmptyMediaStateCard() {
    val spacing = SparkCutTheme.spacing
    val typography = SparkCutTheme.typography
    val colors = SparkCutTheme.colors

    SparkCutCard(
        modifier = Modifier.fillMaxWidth(),
        tone = SparkCutCardTone.Default
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Collections,
                    contentDescription = null,
                    tint = colors.textSecondary
                )
            }

            Text(
                text = "No media selected yet",
                style = typography.cardTitle,
                color = colors.textPrimary
            )

            Text(
                text = "Pick photos or short clips to start building this project.",
                style = typography.body,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun SelectedMediaCard(
    item: CreateContract.SelectedMediaItem,
    thumbnailProvider: MediaThumbnailProvider,
    onRemove: () -> Unit,
) {
    val spacing = SparkCutTheme.spacing
    val typography = SparkCutTheme.typography
    val colors = SparkCutTheme.colors

    SparkCutCard(
        modifier = Modifier.fillMaxWidth(),
        tone = SparkCutCardTone.Elevated
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaThumbnailBox(
                uri = item.uri,
                isVideo = item.isVideo,
                thumbnailProvider = thumbnailProvider,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    SparkCutStatusChip(
                        text = "Item ${item.order + 1}",
                        tone = SparkCutStatusTone.Neutral
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

                SparkCutSecondaryButton(
                    text = "Remove",
                    onClick = onRemove,
                    modifier = Modifier.fillMaxWidth(),
                    size = SparkCutButtonSize.Medium,
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
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
private fun MediaThumbnailBox(
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
        contentAlignment = Alignment.Center
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

private fun bottomBarStatusText(state: CreateContract.State): String {
    return when {
        state.isResolvingMedia -> "Processing"
        state.canContinue -> "Ready"
        else -> "Needs media"
    }
}

private fun bottomBarStatusTone(state: CreateContract.State): SparkCutStatusTone {
    return when {
        state.isResolvingMedia -> SparkCutStatusTone.Info
        state.canContinue -> SparkCutStatusTone.Success
        else -> SparkCutStatusTone.Warning
    }
}

private fun bottomBarSummaryText(state: CreateContract.State): String {
    return when {
        state.isResolvingMedia -> {
            "SparkCut is reading your selected media and preparing the project."
        }

        state.canContinue && state.canAddMore -> {
            "Your project is ready. You can continue now or add up to ${state.remainingSelectableCount} more item${if (state.remainingSelectableCount == 1) "" else "s"}."
        }

        state.canContinue -> {
            "Your project is ready to open in the editor."
        }

        else -> {
            val remaining = (state.minRequiredCount - state.selectedCount).coerceAtLeast(0)
            "Add $remaining more item${if (remaining == 1) "" else "s"} to continue."
        }
    }
}