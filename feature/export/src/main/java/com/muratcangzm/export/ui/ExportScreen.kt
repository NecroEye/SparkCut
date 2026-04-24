package com.muratcangzm.export.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    )
}

@Composable
private fun ExportScreenContent(
    state: ExportContract.State,
    snackbarHostState: SnackbarHostState,
    onEvent: (ExportContract.Event) -> Unit,
) {
    val spacing = SparkCutTheme.spacing
    val colors = SparkCutTheme.colors

    SparkCutScaffold(
        snackbarHostState = snackbarHostState,
        topBar = {
            SparkCutTopBar(
                title = "Export",
                subtitle = state.template?.name ?: "Render project",
                navigationIcon = {
                    IconButton(
                        onClick = { onEvent(ExportContract.Event.BackClicked) }
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
                ExportBottomBar(
                    state = state,
                    onStartExport = {
                        onEvent(ExportContract.Event.StartExportClicked)
                    },
                    onSaveToGallery = {
                        onEvent(ExportContract.Event.SaveToGalleryClicked)
                    },
                    onShare = {
                        onEvent(ExportContract.Event.ShareClicked)
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
                            title = "Export unavailable",
                            message = state.errorMessage ?: "The export screen could not be prepared.",
                            severity = SparkCutValidationBannerSeverity.Error
                        )
                    }

                    item {
                        SparkCutSecondaryButton(
                            text = "Go back",
                            onClick = { onEvent(ExportContract.Event.BackClicked) },
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
                        ExportHeaderCard(
                            template = state.template,
                            state = state
                        )
                    }

                    if (!state.errorMessage.isNullOrBlank()) {
                        item {
                            SparkCutValidationBanner(
                                title = "Export blocked",
                                message = state.errorMessage,
                                severity = SparkCutValidationBannerSeverity.Error
                            )
                        }
                    }

                    item {
                        ExportSectionCard(
                            title = "Project summary",
                            subtitle = "Everything included in the final render.",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.SettingsSuggest,
                                    contentDescription = null,
                                    tint = colors.textPrimary
                                )
                            }
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(spacing.xs)
                            ) {
                                ExportMetaRow("Media items", state.mediaCount.toString())
                                ExportMetaRow("Edited text fields", state.textValueCount.toString())
                                ExportMetaRow("Transition", state.transitionLabel)
                                ExportMetaRow(
                                    "Aspect ratio",
                                    state.template.aspectRatioLabel
                                )
                            }
                        }
                    }

                    item {
                        ExportSectionCard(
                            title = "Export preset",
                            subtitle = "Choose output quality and playback smoothness.",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Speed,
                                    contentDescription = null,
                                    tint = colors.textPrimary
                                )
                            }
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(spacing.sm)
                            ) {
                                state.presets.forEach { preset ->
                                    ExportPresetCard(
                                        preset = preset,
                                        onClick = {
                                            onEvent(ExportContract.Event.PresetSelected(preset.id))
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        ExportSectionCard(
                            title = "Output",
                            subtitle = "Track export progress and publishing state.",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Subtitles,
                                    contentDescription = null,
                                    tint = colors.textPrimary
                                )
                            }
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(spacing.sm)
                            ) {
                                SparkCutValidationBanner(
                                    title = exportOutputBannerTitle(state),
                                    message = exportOutputBannerMessage(state),
                                    severity = exportOutputBannerSeverity(state)
                                )

                                if (state.isExporting) {
                                    LinearProgressIndicator(
                                        progress = { state.progress },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Text(
                                        text = "${state.progressPercent}% • ${state.statusText}",
                                        style = SparkCutTheme.typography.meta,
                                        color = colors.textMuted
                                    )
                                }

                                state.completedOutputPath?.let { outputPath ->
                                    Text(
                                        text = "Cache output: $outputPath",
                                        style = SparkCutTheme.typography.meta,
                                        color = colors.textMuted
                                    )
                                }

                                state.publishedMediaUri?.let { publishedUri ->
                                    SparkCutStatusChip(
                                        text = "Saved to gallery",
                                        tone = SparkCutStatusTone.Success
                                    )
                                    Text(
                                        text = publishedUri,
                                        style = SparkCutTheme.typography.meta,
                                        color = colors.textMuted
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.navigationBarsPadding())
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportBottomBar(
    state: ExportContract.State,
    onStartExport: () -> Unit,
    onSaveToGallery: () -> Unit,
    onShare: () -> Unit,
) {
    val spacing = SparkCutTheme.spacing
    val typography = SparkCutTheme.typography
    val colors = SparkCutTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
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
                        text = exportBottomBarStatusText(state),
                        tone = exportBottomBarStatusTone(state)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = if (state.isExporting) {
                            "${state.progressPercent}%"
                        } else {
                            state.statusText
                        },
                        style = typography.meta,
                        color = colors.textMuted
                    )
                }

                if (state.isExporting) {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text(
                    text = exportBottomBarSummaryText(state),
                    style = typography.body,
                    color = colors.textSecondary
                )

                SparkCutPrimaryButton(
                    text = if (state.isExporting) {
                        "Exporting..."
                    } else {
                        "Start export"
                    },
                    onClick = onStartExport,
                    enabled = state.canStartExport,
                    loading = state.isExporting,
                    size = SparkCutButtonSize.Large,
                    modifier = Modifier.fillMaxWidth(),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.MovieCreation,
                            contentDescription = null
                        )
                    }
                )

                if (!state.completedOutputPath.isNullOrBlank()) {
                    SparkCutSecondaryButton(
                        text = when {
                            state.isPublishing -> "Saving to gallery..."
                            state.publishedMediaUri != null -> "Saved to gallery"
                            else -> "Save to gallery"
                        },
                        onClick = onSaveToGallery,
                        enabled = state.canSaveToGallery,
                        loading = state.isPublishing,
                        modifier = Modifier.fillMaxWidth(),
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Outlined.SaveAlt,
                                contentDescription = null
                            )
                        }
                    )

                    SparkCutSecondaryButton(
                        text = "Share video",
                        onClick = onShare,
                        enabled = state.canShare,
                        modifier = Modifier.fillMaxWidth(),
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Outlined.IosShare,
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportHeaderCard(
    template: ExportContract.TemplateSummary,
    state: ExportContract.State,
) {
    val spacing = SparkCutTheme.spacing
    val typography = SparkCutTheme.typography
    val colors = SparkCutTheme.colors
    val selectedPreset = state.presets.firstOrNull { it.isSelected }

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
                    text = selectedPreset?.label ?: "Preset pending",
                    tone = if (selectedPreset != null) {
                        SparkCutStatusTone.Success
                    } else {
                        SparkCutStatusTone.Warning
                    }
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
                    text = "${state.mediaCount} media",
                    tone = SparkCutStatusTone.Neutral
                )
                SparkCutStatusChip(
                    text = "${state.textValueCount} text fields",
                    tone = SparkCutStatusTone.Neutral
                )
                SparkCutStatusChip(
                    text = state.transitionLabel,
                    tone = SparkCutStatusTone.Accent
                )
            }
        }
    }
}

@Composable
private fun ExportSectionCard(
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
                    contentAlignment = Alignment.Center
                ) {
                    leadingIcon()
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.xxs)
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
private fun ExportPresetCard(
    preset: ExportContract.PresetItem,
    onClick: () -> Unit,
) {
    val spacing = SparkCutTheme.spacing
    val typography = SparkCutTheme.typography
    val colors = SparkCutTheme.colors

    SparkCutCard(
        modifier = Modifier.fillMaxWidth(),
        tone = if (preset.isSelected) {
            SparkCutCardTone.Focused
        } else {
            SparkCutCardTone.Default
        },
        onClick = onClick
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(spacing.xxs)
                ) {
                    Text(
                        text = preset.label,
                        style = typography.cardTitle,
                        color = colors.textPrimary
                    )
                    Text(
                        text = preset.detail,
                        style = typography.meta,
                        color = colors.textMuted
                    )
                }

                FilterChip(
                    selected = preset.isSelected,
                    onClick = onClick,
                    label = {
                        Text(if (preset.isSelected) "Selected" else "Select")
                    }
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                SparkCutStatusChip(
                    text = preset.resolution.name.replace('_', ' '),
                    tone = SparkCutStatusTone.Info
                )
                SparkCutStatusChip(
                    text = preset.fps.name.replace("FPS_", "") + " FPS",
                    tone = SparkCutStatusTone.Neutral
                )
            }
        }
    }
}


@Composable
private fun ExportMetaRow(
    label: String,
    value: String,
) {
    val typography = SparkCutTheme.typography
    val colors = SparkCutTheme.colors

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = typography.body,
            color = colors.textSecondary
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = typography.bodyStrong,
            color = colors.textPrimary
        )
    }
}


private fun exportOutputBannerTitle(state: ExportContract.State): String {
    return when {
        state.isExporting -> "Export in progress"
        !state.errorMessage.isNullOrBlank() -> "Export blocked"
        !state.publishedMediaUri.isNullOrBlank() -> "Saved to gallery"
        !state.completedOutputPath.isNullOrBlank() -> "Export completed"
        state.canStartExport -> "Ready to export"
        else -> "Review export settings"
    }
}

private fun exportOutputBannerMessage(state: ExportContract.State): String {
    return when {
        state.isExporting -> "${state.progressPercent}% completed • ${state.statusText}"
        !state.errorMessage.isNullOrBlank() -> state.errorMessage
        !state.publishedMediaUri.isNullOrBlank() -> "The exported video has been published and is ready to share."
        !state.completedOutputPath.isNullOrBlank() -> "The video file is ready. Save it to gallery to enable sharing."
        state.canStartExport -> "Preset, project data and soundtrack settings are ready."
        else -> state.statusText.ifBlank { "Complete the remaining export requirements." }
    }
}

private fun exportOutputBannerSeverity(state: ExportContract.State): SparkCutValidationBannerSeverity {
    return when {
        state.isExporting -> SparkCutValidationBannerSeverity.Info
        !state.errorMessage.isNullOrBlank() -> SparkCutValidationBannerSeverity.Error
        state.canStartExport || !state.completedOutputPath.isNullOrBlank() -> SparkCutValidationBannerSeverity.Info
        else -> SparkCutValidationBannerSeverity.Warning
    }
}

private fun exportBottomBarStatusText(state: ExportContract.State): String {
    return when {
        state.isExporting -> "Exporting"
        !state.errorMessage.isNullOrBlank() -> "Blocked"
        !state.publishedMediaUri.isNullOrBlank() -> "Published"
        !state.completedOutputPath.isNullOrBlank() -> "Completed"
        state.canStartExport -> "Ready"
        else -> "Review"
    }
}

private fun exportBottomBarStatusTone(state: ExportContract.State): SparkCutStatusTone {
    return when {
        state.isExporting -> SparkCutStatusTone.Info
        !state.errorMessage.isNullOrBlank() -> SparkCutStatusTone.Error
        !state.publishedMediaUri.isNullOrBlank() -> SparkCutStatusTone.Success
        !state.completedOutputPath.isNullOrBlank() -> SparkCutStatusTone.Success
        state.canStartExport -> SparkCutStatusTone.Success
        else -> SparkCutStatusTone.Warning
    }
}

private fun exportBottomBarSummaryText(state: ExportContract.State): String {
    return when {
        state.isExporting -> "${state.progressPercent}% completed • ${state.statusText}"
        !state.errorMessage.isNullOrBlank() -> state.errorMessage
        !state.publishedMediaUri.isNullOrBlank() -> "Your export is saved to gallery and ready to share."
        !state.completedOutputPath.isNullOrBlank() -> "Export finished. Save it to gallery to enable sharing."
        state.canStartExport -> "Everything looks good. Start export when you are ready."
        else -> state.statusText.ifBlank { "Check your export configuration before continuing." }
    }
}