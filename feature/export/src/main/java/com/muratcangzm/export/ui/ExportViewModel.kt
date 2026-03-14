package com.muratcangzm.export.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muratcangzm.data.projectsession.ProjectSessionManager
import com.muratcangzm.media.domain.MediaAssetResolver
import com.muratcangzm.media.domain.export.AudioMixRequest
import com.muratcangzm.media.domain.export.AudioTrackMetadataReader
import com.muratcangzm.media.domain.export.BackgroundAudioTrackRequest
import com.muratcangzm.media.domain.export.ExportTextGravity
import com.muratcangzm.media.domain.export.ExportTextOverlay
import com.muratcangzm.media.domain.export.ExportVisualStyle
import com.muratcangzm.media.domain.export.MediaExportEngine
import com.muratcangzm.media.domain.export.MediaExportFilePublisher
import com.muratcangzm.media.domain.export.MediaExportRequest
import com.muratcangzm.media.domain.export.MediaExportSequenceItem
import com.muratcangzm.media.domain.export.MediaExportSession
import com.muratcangzm.media.domain.export.MediaExportState
import com.muratcangzm.model.export.AudioCodec
import com.muratcangzm.model.export.ExportFps
import com.muratcangzm.model.export.ExportPreset
import com.muratcangzm.model.export.ExportResolution
import com.muratcangzm.model.export.VideoCodec
import com.muratcangzm.model.id.ExportJobId
import com.muratcangzm.model.id.ProjectId
import com.muratcangzm.model.id.TemplateId
import com.muratcangzm.model.media.MediaType
import com.muratcangzm.model.project.ProjectStatus
import com.muratcangzm.model.template.AspectRatio
import com.muratcangzm.model.template.AudioTrimBehavior
import com.muratcangzm.model.template.TemplateCategory
import com.muratcangzm.model.template.TemplateSpec
import com.muratcangzm.model.template.TransitionPreset
import com.muratcangzm.templateengine.catalog.TemplateCatalog
import com.muratcangzm.templateengine.planner.PlannedOverlayGravity
import com.muratcangzm.templateengine.planner.TemplateRenderPlanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ExportViewModel(
    private val templateCatalog: TemplateCatalog,
    private val mediaExportEngine: MediaExportEngine,
    private val mediaExportFilePublisher: MediaExportFilePublisher,
    private val mediaAssetResolver: MediaAssetResolver,
    private val templateRenderPlanner: TemplateRenderPlanner,
    private val launchArgs: ExportContract.LaunchArgs,
    private val audioTrackMetadataReader: AudioTrackMetadataReader,
    private val projectSessionManager: ProjectSessionManager,
) : ViewModel(), ExportContract.Presenter {

    private val _state = MutableStateFlow(ExportContract.State())
    override val state: StateFlow<ExportContract.State> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ExportContract.Effect>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val effects: SharedFlow<ExportContract.Effect> = _effects.asSharedFlow()

    private var loadedTemplate: TemplateSpec? = null
    private var activeSession: MediaExportSession? = null

    private val exportPresets: List<ExportPreset> = listOf(
        ExportPreset(
            id = "social_hd_30",
            label = "Social HD",
            resolution = ExportResolution.HD_720,
            fps = ExportFps.FPS_30,
            videoCodec = VideoCodec.H264,
            audioCodec = AudioCodec.AAC,
            videoBitrateMbps = 6,
            audioBitrateKbps = 192,
            includeWatermark = false,
        ),
        ExportPreset(
            id = "social_fhd_30",
            label = "Full HD",
            resolution = ExportResolution.FHD_1080,
            fps = ExportFps.FPS_30,
            videoCodec = VideoCodec.H264,
            audioCodec = AudioCodec.AAC,
            videoBitrateMbps = 10,
            audioBitrateKbps = 192,
            includeWatermark = false,
        ),
        ExportPreset(
            id = "smooth_fhd_60",
            label = "Smooth 60 FPS",
            resolution = ExportResolution.FHD_1080,
            fps = ExportFps.FPS_60,
            videoCodec = VideoCodec.H264,
            audioCodec = AudioCodec.AAC,
            videoBitrateMbps = 16,
            audioBitrateKbps = 256,
            includeWatermark = false,
        ),
    )

    init {
        load()
    }

    override fun onEvent(event: ExportContract.Event) {
        when (event) {
            ExportContract.Event.BackClicked -> {
                if (_state.value.isExporting) {
                    activeSession?.cancel()
                } else {
                    _effects.tryEmit(ExportContract.Effect.NavigateBack)
                }
            }

            is ExportContract.Event.PresetSelected -> {
                _state.update { current ->
                    current.copy(
                        selectedPresetId = event.presetId,
                        presets = current.presets.map { item ->
                            item.copy(isSelected = item.id == event.presetId)
                        },
                    )
                }
            }

            ExportContract.Event.StartExportClicked -> {
                startRealExport()
            }

            ExportContract.Event.SaveToGalleryClicked -> {
                saveToGallery()
            }

            ExportContract.Event.ShareClicked -> {
                val uri = _state.value.publishedMediaUri
                if (uri.isNullOrBlank()) {
                    _effects.tryEmit(
                        ExportContract.Effect.ShowMessage(
                            "Save the exported video to gallery before sharing.",
                        ),
                    )
                } else {
                    _effects.tryEmit(ExportContract.Effect.ShareMedia(uri))
                }
            }

            ExportContract.Event.PickMusicClicked -> {
                _effects.tryEmit(ExportContract.Effect.OpenAudioPicker)
            }

            is ExportContract.Event.MusicPicked -> {
                val normalized = event.uri?.trim()?.takeIf(String::isNotBlank)
                _state.update {
                    it.copy(
                        backgroundMusicUri = normalized,
                        soundtrackDisplayName = null,
                        soundtrackDurationLabel = null,
                        soundtrackMimeType = null,
                        soundtrackErrorMessage = null,
                        isLoadingSoundtrackMetadata = normalized != null,
                    )
                }

                if (normalized != null) {
                    loadSoundtrackMetadata(normalized)
                }
            }

            ExportContract.Event.ClearMusicClicked -> {
                _state.update {
                    it.copy(
                        backgroundMusicUri = null,
                        soundtrackDisplayName = null,
                        soundtrackDurationLabel = null,
                        soundtrackMimeType = null,
                        soundtrackErrorMessage = null,
                        isLoadingSoundtrackMetadata = false,
                    )
                }
            }

            is ExportContract.Event.ClipAudioVolumeChanged -> {
                _state.update {
                    it.copy(
                        clipAudioVolume = event.value.coerceIn(0f, 1f),
                    )
                }
            }

            is ExportContract.Event.MusicAudioVolumeChanged -> {
                _state.update {
                    it.copy(
                        musicAudioVolume = event.value.coerceIn(0f, 1f),
                    )
                }
            }

            is ExportContract.Event.FadeInChanged -> {
                _state.update {
                    it.copy(
                        fadeInMs = event.valueMs.coerceAtLeast(0L),
                    )
                }
            }

            is ExportContract.Event.FadeOutChanged -> {
                _state.update {
                    it.copy(
                        fadeOutMs = event.valueMs.coerceAtLeast(0L),
                    )
                }
            }

            is ExportContract.Event.ProgressUpdated -> Unit
            ExportContract.Event.FakeExportCompleted -> Unit
        }
    }

    private fun startRealExport() {
        val current = _state.value
        if (!current.canStartExport) {
            _effects.tryEmit(
                ExportContract.Effect.ShowMessage(
                    "Select a preset and make sure your export data is valid.",
                ),
            )
            return
        }

        val preset = exportPresets.firstOrNull { it.id == current.selectedPresetId }
        if (preset == null) {
            _effects.tryEmit(
                ExportContract.Effect.ShowMessage("Selected preset could not be resolved."),
            )
            return
        }

        val template = loadedTemplate
        if (template == null) {
            _effects.tryEmit(
                ExportContract.Effect.ShowMessage("Template data is missing."),
            )
            return
        }

        val transition = parseTransition(launchArgs.transitionPresetName) ?: TransitionPreset.FADE
        val persistedProjectId = launchArgs.projectId?.takeIf { it.isNotBlank() }?.let(::ProjectId)

        _state.update {
            it.copy(
                isExporting = true,
                progress = 0f,
                statusText = "Planning export…",
                errorMessage = null,
                completedOutputPath = null,
                publishedMediaUri = null,
            )
        }

        viewModelScope.launch {
            persistedProjectId?.let {
                projectSessionManager.updateStatus(
                    projectId = it,
                    status = ProjectStatus.EXPORTING,
                    updatedAtEpochMillis = System.currentTimeMillis(),
                )
            }

            val resolved = withContext(Dispatchers.IO) {
                mediaAssetResolver.resolveAll(
                    launchArgs.mediaClips.map { it.uri },
                )
            }

            if (resolved.assets.isEmpty()) {
                persistedProjectId?.let {
                    projectSessionManager.updateStatus(
                        projectId = it,
                        status = ProjectStatus.FAILED,
                        updatedAtEpochMillis = System.currentTimeMillis(),
                    )
                }

                _state.update {
                    it.copy(
                        isExporting = false,
                        progress = 0f,
                        statusText = "Export failed",
                        errorMessage = "No readable media items were found.",
                    )
                }
                _effects.tryEmit(
                    ExportContract.Effect.ShowMessage("No readable media items were found."),
                )
                return@launch
            }

            if (resolved.failures.isNotEmpty()) {
                _effects.tryEmit(
                    ExportContract.Effect.ShowMessage(
                        "${resolved.failures.size} item(s) could not be read and were skipped.",
                    ),
                )
            }

            val clipArgByUri = launchArgs.mediaClips.associateBy { it.uri }

            val trimmedAssets = resolved.assets.map { asset ->
                val clipArg = clipArgByUri[asset.uri]
                if (asset.type != MediaType.VIDEO || clipArg == null) {
                    asset
                } else {
                    val sourceDuration = asset.durationMs ?: 0L
                    val userStart = (clipArg.trimStartMs ?: 0L).coerceIn(0L, sourceDuration)
                    val userEnd = (clipArg.trimEndMs ?: sourceDuration).coerceIn(userStart, sourceDuration)
                    val effectiveDuration = (userEnd - userStart).coerceAtLeast(300L)

                    asset.copy(
                        durationMs = effectiveDuration,
                    )
                }
            }

            val textMap = launchArgs.textValues.associate { it.fieldId to it.value }

            val renderPlan = templateRenderPlanner.createPlan(
                template = template,
                assets = trimmedAssets,
                textValues = textMap,
            )

            if (renderPlan.sequenceItems.isEmpty()) {
                persistedProjectId?.let {
                    projectSessionManager.updateStatus(
                        projectId = it,
                        status = ProjectStatus.FAILED,
                        updatedAtEpochMillis = System.currentTimeMillis(),
                    )
                }

                _state.update {
                    it.copy(
                        isExporting = false,
                        progress = 0f,
                        statusText = "Export failed",
                        errorMessage = "Could not build a valid render plan.",
                    )
                }
                _effects.tryEmit(
                    ExportContract.Effect.ShowMessage("Could not build a valid render plan."),
                )
                return@launch
            }

            val backgroundMusicUri = current.backgroundMusicUri
                ?.trim()
                ?.takeIf { it.isNotBlank() }

            val shouldLoopBackgroundAudio: Boolean = when (template.musicPolicy.trimBehavior) {
                AudioTrimBehavior.LOOP,
                AudioTrimBehavior.AUTO_FIT -> true
                else -> false
            }

            val preserveOriginalClipAudio = current.clipAudioVolume > 0.01f &&
                    template.musicPolicy.preserveOriginalClipAudio

            val request = MediaExportRequest(
                jobId = ExportJobId(UUID.randomUUID().toString()),
                sequenceItems = renderPlan.sequenceItems.map { item ->
                    val clipArg = clipArgByUri[item.uri]
                    val baseTrimStartMs = clipArg?.trimStartMs ?: 0L

                    MediaExportSequenceItem(
                        uri = item.uri,
                        mediaType = item.mediaType,
                        durationMs = item.durationMs,
                        trimStartMs = if (item.mediaType == MediaType.VIDEO) {
                            baseTrimStartMs + (item.trimStartMs ?: 0L)
                        } else {
                            null
                        },
                        trimEndMs = if (item.mediaType == MediaType.VIDEO) {
                            baseTrimStartMs + (item.trimEndMs ?: item.durationMs)
                        } else {
                            null
                        },
                    )
                },
                preset = preset,
                textValues = textMap,
                transitionPreset = transition,
                textOverlays = renderPlan.textOverlays.map { overlay ->
                    ExportTextOverlay(
                        id = overlay.id,
                        text = overlay.text,
                        gravity = when (overlay.gravity) {
                            PlannedOverlayGravity.TOP_CENTER -> ExportTextGravity.TOP_CENTER
                            PlannedOverlayGravity.CENTER -> ExportTextGravity.CENTER
                            PlannedOverlayGravity.BOTTOM_CENTER -> ExportTextGravity.BOTTOM_CENTER
                        },
                        startTimeMs = overlay.startTimeMs,
                        endTimeMs = overlay.endTimeMs,
                        textSizeSp = overlay.textSizeSp,
                    )
                },
                transitionWindows = renderPlan.transitions.map { transitionWindow ->
                    com.muratcangzm.media.domain.export.MediaTransitionWindow(
                        preset = transitionWindow.preset,
                        targetIndex = transitionWindow.targetIndex,
                        phase = when (transitionWindow.phase) {
                            com.muratcangzm.templateengine.planner.TransitionPhase.INTRO ->
                                com.muratcangzm.media.domain.export.MediaTransitionPhase.INTRO

                            com.muratcangzm.templateengine.planner.TransitionPhase.OUTRO ->
                                com.muratcangzm.media.domain.export.MediaTransitionPhase.OUTRO
                        },
                        durationMs = transitionWindow.durationMs,
                    )
                },
                visualStyle = ExportVisualStyle(
                    presetName = transition.name,
                ),
                audioMix = AudioMixRequest(
                    preserveOriginalClipAudio = preserveOriginalClipAudio,
                    backgroundTrack = backgroundMusicUri?.let { uri ->
                        BackgroundAudioTrackRequest(
                            uri = uri,
                            loop = shouldLoopBackgroundAudio,
                        )
                    },
                    clipAudioVolume = current.clipAudioVolume,
                    backgroundAudioVolume = current.musicAudioVolume,
                    fadeInMs = current.fadeInMs,
                    fadeOutMs = current.fadeOutMs,
                ),
                outputFileBaseName = template.name,
            )

            val session = mediaExportEngine.createSession(request)
            activeSession = session

            viewModelScope.launch {
                session.state.collectLatest { exportState ->
                    when (exportState) {
                        MediaExportState.Idle -> Unit

                        is MediaExportState.Running -> {
                            _state.update {
                                it.copy(
                                    isExporting = true,
                                    progress = exportState.progress,
                                    statusText = exportState.statusText,
                                )
                            }
                        }

                        is MediaExportState.Completed -> {
                            persistedProjectId?.let {
                                projectSessionManager.updateStatus(
                                    projectId = it,
                                    status = ProjectStatus.EXPORTED,
                                    updatedAtEpochMillis = System.currentTimeMillis(),
                                )
                            }

                            _state.update {
                                it.copy(
                                    isExporting = false,
                                    progress = 1f,
                                    statusText = "Export completed",
                                    completedOutputPath = exportState.outputFilePath,
                                )
                            }
                            _effects.tryEmit(
                                ExportContract.Effect.ShowMessage(
                                    "Export completed. You can now save it to gallery.",
                                ),
                            )
                        }

                        is MediaExportState.Failed -> {
                            persistedProjectId?.let {
                                projectSessionManager.updateStatus(
                                    projectId = it,
                                    status = ProjectStatus.FAILED,
                                    updatedAtEpochMillis = System.currentTimeMillis(),
                                )
                            }

                            _state.update {
                                it.copy(
                                    isExporting = false,
                                    progress = 0f,
                                    statusText = "Export failed",
                                    errorMessage = exportState.message,
                                )
                            }
                            _effects.tryEmit(
                                ExportContract.Effect.ShowMessage(exportState.message),
                            )
                        }

                        is MediaExportState.Cancelled -> {
                            persistedProjectId?.let {
                                projectSessionManager.updateStatus(
                                    projectId = it,
                                    status = ProjectStatus.READY,
                                    updatedAtEpochMillis = System.currentTimeMillis(),
                                )
                            }

                            _state.update {
                                it.copy(
                                    isExporting = false,
                                    progress = 0f,
                                    statusText = "Export cancelled",
                                )
                            }
                            _effects.tryEmit(
                                ExportContract.Effect.ShowMessage("Export cancelled."),
                            )
                        }
                    }
                }
            }

            session.start()
        }
    }

    private fun saveToGallery() {
        val current = _state.value
        val outputPath = current.completedOutputPath

        if (outputPath.isNullOrBlank()) {
            _effects.tryEmit(
                ExportContract.Effect.ShowMessage("No completed export file found."),
            )
            return
        }

        val template = current.template
        if (template == null) {
            _effects.tryEmit(
                ExportContract.Effect.ShowMessage("Template info is missing."),
            )
            return
        }

        _state.update {
            it.copy(isPublishing = true)
        }

        viewModelScope.launch {
            runCatching {
                mediaExportFilePublisher.publishVideo(
                    inputFilePath = outputPath,
                    desiredDisplayName = template.name,
                )
            }.onSuccess { published ->
                _state.update {
                    it.copy(
                        isPublishing = false,
                        publishedMediaUri = published.contentUri,
                        statusText = "Saved to gallery",
                    )
                }
                _effects.tryEmit(
                    ExportContract.Effect.ShowMessage(
                        "Saved to gallery: ${published.displayName}",
                    ),
                )
            }.onFailure { throwable ->
                _state.update {
                    it.copy(isPublishing = false)
                }
                _effects.tryEmit(
                    ExportContract.Effect.ShowMessage(
                        throwable.message ?: "Could not save video to gallery.",
                    ),
                )
            }
        }
    }

    private fun loadSoundtrackMetadata(uri: String) {
        viewModelScope.launch {
            val metadata = audioTrackMetadataReader.read(uri)

            if (metadata == null) {
                _state.update {
                    it.copy(
                        soundtrackDisplayName = null,
                        soundtrackDurationLabel = null,
                        soundtrackMimeType = null,
                        soundtrackErrorMessage = "Selected soundtrack could not be read.",
                        isLoadingSoundtrackMetadata = false,
                    )
                }
                _effects.tryEmit(
                    ExportContract.Effect.ShowMessage(
                        "Selected soundtrack could not be read.",
                    ),
                )
                return@launch
            }

            val mimeType = metadata.mimeType
            val durationMs = metadata.durationMs ?: 0L

            val validationError = when {
                mimeType != null && !mimeType.startsWith("audio/") ->
                    "Selected file is not a supported audio track."

                durationMs <= 0L ->
                    "Selected soundtrack has invalid duration."

                durationMs < 1_500L ->
                    "Selected soundtrack is too short. Pick a longer track."

                else -> null
            }

            _state.update {
                it.copy(
                    soundtrackDisplayName = metadata.displayName,
                    soundtrackDurationLabel = metadata.durationMs?.toDurationLabel(),
                    soundtrackMimeType = metadata.mimeType,
                    soundtrackErrorMessage = validationError,
                    isLoadingSoundtrackMetadata = false,
                )
            }

            if (validationError != null) {
                _effects.tryEmit(
                    ExportContract.Effect.ShowMessage(validationError),
                )
            }
        }
    }

    private fun load() {
        val templateId = runCatching { TemplateId(launchArgs.templateId) }.getOrNull()
        if (templateId == null) {
            _state.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Invalid template id.",
                )
            }
            return
        }

        val template = templateCatalog.getById(templateId)
        if (template == null) {
            _state.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Template not found.",
                )
            }
            return
        }

        loadedTemplate = template

        val selectedTransition = parseTransition(launchArgs.transitionPresetName)
            ?: template.defaultTransition

        val presetItems = exportPresets.mapIndexed { index, preset ->
            preset.toUi(isSelected = index == 1)
        }

        _state.update {
            it.copy(
                isLoading = false,
                template = template.toSummary(),
                mediaCount = launchArgs.mediaClips.distinctBy { clip -> clip.uri }.size,
                textValueCount = launchArgs.textValues.size,
                transitionLabel = selectedTransition.displayLabel(),
                presets = presetItems,
                selectedPresetId = presetItems.firstOrNull { item -> item.isSelected }?.id,
                backgroundMusicUri = launchArgs.backgroundMusicUri,
                clipAudioVolume = template.musicPolicy.clipAudioVolume.coerceIn(0f, 1f),
                musicAudioVolume = template.musicPolicy.musicVolume.coerceIn(0f, 1f),
                fadeInMs = 600L,
                fadeOutMs = 600L,
                statusText = "Ready to export",
                errorMessage = if (launchArgs.mediaClips.isEmpty()) {
                    "No media found for export."
                } else {
                    null
                },
            )
        }

        val initialMusicUri = launchArgs.backgroundMusicUri?.trim()?.takeIf { it.isNotBlank() }
        if (initialMusicUri != null) {
            _state.update {
                it.copy(isLoadingSoundtrackMetadata = true)
            }
            loadSoundtrackMetadata(initialMusicUri)
        }
    }

    private fun parseTransition(raw: String): TransitionPreset? =
        enumValues<TransitionPreset>().firstOrNull { preset ->
            preset.name.equals(raw, ignoreCase = true)
        }
}

private fun ExportPreset.toUi(
    isSelected: Boolean,
): ExportContract.PresetItem = ExportContract.PresetItem(
    id = id,
    label = label,
    detail = "${resolution.displayLabel()} • ${fps.displayLabel()} • $videoBitrateMbps Mbps",
    resolution = resolution,
    fps = fps,
    isSelected = isSelected,
)

private fun TemplateSpec.toSummary(): ExportContract.TemplateSummary =
    ExportContract.TemplateSummary(
        id = id,
        name = name,
        description = description,
        categoryLabel = category.displayLabel(),
        aspectRatioLabel = aspectRatio.displayLabel(),
    )

private fun ExportResolution.displayLabel(): String = when (this) {
    ExportResolution.HD_720 -> "720p"
    ExportResolution.FHD_1080 -> "1080p"
    ExportResolution.UHD_4K -> "4K"
}

private fun ExportFps.displayLabel(): String = when (this) {
    ExportFps.FPS_24 -> "24 FPS"
    ExportFps.FPS_30 -> "30 FPS"
    ExportFps.FPS_60 -> "60 FPS"
}

private fun TransitionPreset.displayLabel(): String = when (this) {
    TransitionPreset.CUT -> "Cut"
    TransitionPreset.FADE -> "Fade"
    TransitionPreset.SLIDE_LEFT -> "Slide Left"
    TransitionPreset.SLIDE_RIGHT -> "Slide Right"
    TransitionPreset.ZOOM_IN -> "Zoom In"
    TransitionPreset.ZOOM_OUT -> "Zoom Out"
    TransitionPreset.GLITCH_RGB -> "Glitch RGB"
    TransitionPreset.SHAKE -> "Shake"
    TransitionPreset.FLASH -> "Flash"
    TransitionPreset.BLUR -> "Blur"
}

private fun TemplateCategory.displayLabel(): String = when (this) {
    TemplateCategory.TRENDING -> "Trending"
    TemplateCategory.PARTY -> "Party"
    TemplateCategory.LOVE -> "Love"
    TemplateCategory.TRAVEL -> "Travel"
    TemplateCategory.FITNESS -> "Fitness"
    TemplateCategory.PROMO -> "Promo"
    TemplateCategory.GLITCH -> "Glitch"
    TemplateCategory.BIRTHDAY -> "Birthday"
    TemplateCategory.MEMORIES -> "Memories"
    TemplateCategory.BUSINESS -> "Business"
    TemplateCategory.MINIMAL -> "Minimal"
    TemplateCategory.CINEMATIC -> "Cinematic"
}

private fun AspectRatio.displayLabel(): String = when (this) {
    AspectRatio.VERTICAL_9_16 -> "9:16"
    AspectRatio.PORTRAIT_4_5 -> "4:5"
    AspectRatio.SQUARE_1_1 -> "1:1"
    AspectRatio.LANDSCAPE_16_9 -> "16:9"
}

private fun Long.toDurationLabel(): String {
    val totalSeconds = (this / 1000L).toInt().coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
