package com.muratcangzm.export.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ExportScreen(
    launchArgs: ExportContract.LaunchArgs,
    onBack: () -> Unit,
    viewModel: ExportViewModel = koinViewModel(
        parameters = { parametersOf(launchArgs) }
    ),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.onEvent(
                ExportContract.Event.MusicPicked(uri.toString())
            )
        } else {
            viewModel.onEvent(
                ExportContract.Event.MusicPicked(null)
            )
        }
    }

    val previewPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    var isMusicPreviewPlaying by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            previewPlayer.release()
        }
    }

    LaunchedEffect(state.backgroundMusicUri) {
        previewPlayer.stop()
        previewPlayer.clearMediaItems()
        isMusicPreviewPlaying = false

        val musicUri = state.backgroundMusicUri
        if (!musicUri.isNullOrBlank() && state.soundtrackErrorMessage == null) {
            previewPlayer.setMediaItem(MediaItem.fromUri(musicUri))
            previewPlayer.prepare()
        }
    }

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ExportContract.Effect.NavigateBack -> onBack()

                ExportContract.Effect.OpenAudioPicker -> {
                    audioPickerLauncher.launch(arrayOf("audio/*"))
                }

                is ExportContract.Effect.ShareMedia -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "video/mp4"
                        putExtra(Intent.EXTRA_STREAM, Uri.parse(effect.contentUri))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(shareIntent, "Share exported video")
                    )
                }

                is ExportContract.Effect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    ExportScreenContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
        onToggleMusicPreview = {
            val canPreview = state.hasBackgroundMusic &&
                    state.soundtrackErrorMessage == null &&
                    !state.isLoadingSoundtrackMetadata

            if (!canPreview) return@ExportScreenContent

            if (previewPlayer.isPlaying) {
                previewPlayer.pause()
                isMusicPreviewPlaying = false
            } else {
                previewPlayer.playWhenReady = true
                previewPlayer.play()
                isMusicPreviewPlaying = true
            }
        },
        isMusicPreviewPlaying = isMusicPreviewPlaying,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreenContent(
    state: ExportContract.State,
    snackbarHostState: SnackbarHostState,
    onEvent: (ExportContract.Event) -> Unit,
    onToggleMusicPreview: () -> Unit,
    isMusicPreviewPlaying: Boolean,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export") },
                navigationIcon = {
                    IconButton(
                        onClick = { onEvent(ExportContract.Event.BackClicked) }
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
                    if (state.isExporting) {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = "${state.progressPercent}% • ${state.statusText}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = state.statusText,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Button(
                        onClick = { onEvent(ExportContract.Event.StartExportClicked) },
                        enabled = state.canStartExport,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MovieCreation,
                            contentDescription = null,
                        )
                        Text(
                            text = if (state.isExporting) "Exporting..." else "Start export",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }

                    if (!state.completedOutputPath.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = { onEvent(ExportContract.Event.SaveToGalleryClicked) },
                            enabled = state.canSaveToGallery,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SaveAlt,
                                contentDescription = null,
                            )
                            Text(
                                text = when {
                                    state.isPublishing -> "Saving to gallery..."
                                    state.publishedMediaUri != null -> "Saved to gallery"
                                    else -> "Save to gallery"
                                },
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }

                        OutlinedButton(
                            onClick = { onEvent(ExportContract.Event.ShareClicked) },
                            enabled = state.canShare,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.IosShare,
                                contentDescription = null,
                            )
                            Text(
                                text = "Share video",
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
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
                    TemplateExportHeader(template = state.template)
                } else {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = state.errorMessage ?: "Could not prepare export.",
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            item {
                ExportSectionCard(
                    icon = Icons.Outlined.SettingsSuggest,
                    title = "Project summary",
                    subtitle = "What will be used to generate the final video",
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Media items: ${state.mediaCount}")
                        Text("Edited text fields: ${state.textValueCount}")
                        Text("Transition: ${state.transitionLabel}")
                    }
                }
            }

            item {
                ExportSectionCard(
                    icon = Icons.Outlined.Speed,
                    title = "Export preset",
                    subtitle = "Choose the quality and frame rate for your output",
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        state.presets.forEach { preset ->
                            FilterChip(
                                selected = preset.isSelected,
                                onClick = {
                                    onEvent(ExportContract.Event.PresetSelected(preset.id))
                                },
                                label = {
                                    Column {
                                        Text(preset.label)
                                        Text(
                                            text = preset.detail,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }

            item {
                ExportSectionCard(
                    icon = Icons.Outlined.Audiotrack,
                    title = "Soundtrack",
                    subtitle = "Pick an optional background music track and preview it",
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = when {
                                state.isLoadingSoundtrackMetadata -> "Reading soundtrack info..."
                                state.soundtrackErrorMessage != null -> state.soundtrackErrorMessage
                                state.soundtrackDisplayName != null -> state.soundtrackDisplayName
                                else -> "No soundtrack selected"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (state.soundtrackErrorMessage != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )

                        if (!state.soundtrackDurationLabel.isNullOrBlank() || !state.soundtrackMimeType.isNullOrBlank()) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                state.soundtrackDurationLabel?.let { duration ->
                                    Text(
                                        text = "Duration: $duration",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                state.soundtrackMimeType?.let { mime ->
                                    Text(
                                        text = mime,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { onEvent(ExportContract.Event.PickMusicClicked) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Audiotrack,
                                contentDescription = null,
                            )
                            Text(
                                text = if (state.hasBackgroundMusic) {
                                    "Replace soundtrack"
                                } else {
                                    "Pick soundtrack"
                                },
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }

                        OutlinedButton(
                            onClick = onToggleMusicPreview,
                            enabled = state.hasBackgroundMusic &&
                                    state.soundtrackErrorMessage == null &&
                                    !state.isLoadingSoundtrackMetadata,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = if (isMusicPreviewPlaying) {
                                    Icons.Outlined.Pause
                                } else {
                                    Icons.Outlined.PlayArrow
                                },
                                contentDescription = null,
                            )
                            Text(
                                text = if (isMusicPreviewPlaying) {
                                    "Pause preview"
                                } else {
                                    "Play preview"
                                },
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }

                        OutlinedButton(
                            onClick = { onEvent(ExportContract.Event.ClearMusicClicked) },
                            enabled = state.hasBackgroundMusic,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = null,
                            )
                            Text(
                                text = "Clear soundtrack",
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }

            item {
                ExportSectionCard(
                    icon = Icons.Outlined.VolumeUp,
                    title = "Audio mix",
                    subtitle = "Adjust source audio, soundtrack level, and fade timings",
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = "Clip audio: ${(state.clipAudioVolume * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Slider(
                            value = state.clipAudioVolume,
                            onValueChange = {
                                onEvent(ExportContract.Event.ClipAudioVolumeChanged(it))
                            },
                            valueRange = 0f..1f,
                        )

                        Text(
                            text = "Music audio: ${(state.musicAudioVolume * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Slider(
                            value = state.musicAudioVolume,
                            onValueChange = {
                                onEvent(ExportContract.Event.MusicAudioVolumeChanged(it))
                            },
                            valueRange = 0f..1f,
                        )

                        Text(
                            text = "Fade in: ${state.fadeInMs} ms",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Slider(
                            value = state.fadeInMs.toFloat(),
                            onValueChange = {
                                onEvent(
                                    ExportContract.Event.FadeInChanged(it.toLong())
                                )
                            },
                            valueRange = 0f..3000f,
                        )

                        Text(
                            text = "Fade out: ${state.fadeOutMs} ms",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Slider(
                            value = state.fadeOutMs.toFloat(),
                            onValueChange = {
                                onEvent(
                                    ExportContract.Event.FadeOutChanged(it.toLong())
                                )
                            },
                            valueRange = 0f..3000f,
                        )
                    }
                }
            }

            item {
                ExportSectionCard(
                    icon = Icons.Outlined.Subtitles,
                    title = "Output",
                    subtitle = "Export result and publishing state",
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Status: ${state.statusText}")

                        state.completedOutputPath?.let {
                            Text(
                                text = "Cache output: $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        state.publishedMediaUri?.let {
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = { Text("Gallery saved") },
                            )
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateExportHeader(
    template: ExportContract.TemplateSummary,
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
private fun ExportSectionCard(
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
