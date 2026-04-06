package com.muratcangzm.editor.ui

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muratcangzm.data.projectsession.ProjectSessionManager
import com.muratcangzm.media.domain.MediaAssetResolver
import com.muratcangzm.media.domain.MediaDurationFormatter
import com.muratcangzm.model.id.MediaAssetId
import com.muratcangzm.model.id.ProjectId
import com.muratcangzm.model.id.TemplateId
import com.muratcangzm.model.media.MediaAsset
import com.muratcangzm.model.media.MediaType
import com.muratcangzm.model.project.AudioSourceKind
import com.muratcangzm.model.project.ProjectAudioSelection
import com.muratcangzm.model.project.ProjectDraft
import com.muratcangzm.model.project.ProjectEditorSession
import com.muratcangzm.model.project.ProjectMediaAssetRef
import com.muratcangzm.model.project.ProjectSlotBinding
import com.muratcangzm.model.project.ProjectStatus
import com.muratcangzm.model.project.ProjectTextValue
import com.muratcangzm.model.project.ProjectTransitionOverride
import com.muratcangzm.model.template.AspectRatio
import com.muratcangzm.model.template.TransitionPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class EditorViewModel(
    private val mediaAssetResolver: MediaAssetResolver,
    private val mediaUrisArg: List<String>,
    private val projectSessionManager: ProjectSessionManager,
    private val projectIdArg: String?,
    private val applicationContext: android.content.Context,
) : ViewModel(), EditorContract.Presenter {

    private val _state = MutableStateFlow(EditorContract.State())
    override val state: StateFlow<EditorContract.State> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<EditorContract.Effect>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val effects: SharedFlow<EditorContract.Effect> = _effects.asSharedFlow()

    private var currentProjectId: ProjectId? =
        projectIdArg?.takeIf { it.isNotBlank() }?.let(::ProjectId)
    private var createdAtEpochMillis: Long? = null
    private val persistedAssetIdsByUri = linkedMapOf<String, MediaAssetId>()
    private var autosaveJob: Job? = null
    private var autosaveStatusResetJob: Job? = null
    private var photoTimerJob: Job? = null

    init {
        if (currentProjectId != null) {
            loadPersistedProject(currentProjectId!!)
        } else {
            resolveMediaFromUris(mediaUrisArg)
        }
    }

    override fun onEvent(event: EditorContract.Event) {
        when (event) {
            EditorContract.Event.BackClicked -> {
                viewModelScope.launch {
                    persistCurrentSession()
                    _effects.tryEmit(EditorContract.Effect.NavigateBack)
                }
            }

            EditorContract.Event.TogglePlayPause -> {
                val current = _state.value
                if (current.selectedMedia.isEmpty()) return
                if (current.isPlaying) {
                    _state.update { it.copy(isPlaying = false) }
                    _effects.tryEmit(EditorContract.Effect.PausePlayer)
                    photoTimerJob?.cancel()
                } else {
                    val clipIndex = current.currentClipIndex
                    val clip = current.selectedMedia.getOrNull(clipIndex)
                    if (clip != null) {
                        if (clip.isVideo) {
                            _effects.tryEmit(
                                EditorContract.Effect.PreparePlayer(
                                    clipIndex,
                                    current.currentClipLocalPositionMs,
                                )
                            )
                            _state.update { it.copy(isPlaying = true) }
                            _effects.tryEmit(EditorContract.Effect.StartPlayer)
                        } else {
                            _state.update { it.copy(isPlaying = true) }
                            startPhotoTimer(clipIndex)
                        }
                    }
                }
            }

            is EditorContract.Event.SeekTo -> {
                val total = _state.value.totalDurationMs
                val clamped = event.positionMs.coerceIn(0L, total)
                val (clipIndex, localPos) = resolveClipForGlobalPosition(clamped)
                _state.update {
                    it.copy(
                        playbackPositionMs = clamped,
                        currentClipIndex = clipIndex,
                        currentClipLocalPositionMs = localPos,
                        isPlaying = false,
                    )
                }
                _effects.tryEmit(EditorContract.Effect.PausePlayer)
                _effects.tryEmit(EditorContract.Effect.PreparePlayer(clipIndex, localPos))
            }

            EditorContract.Event.SeekBack -> {
                val newPos = (_state.value.playbackPositionMs - 1_000L).coerceAtLeast(0L)
                onEvent(EditorContract.Event.SeekTo(newPos))
            }

            EditorContract.Event.SeekForward -> {
                val newPos = (_state.value.playbackPositionMs + 1_000L)
                    .coerceAtMost(_state.value.totalDurationMs)
                onEvent(EditorContract.Event.SeekTo(newPos))
            }

            EditorContract.Event.ToggleMute -> {
                _state.update { it.copy(isMuted = !it.isMuted) }
            }

            is EditorContract.Event.PlaybackPositionUpdate -> {
                val clipIndex = _state.value.currentClipIndex
                val clips = _state.value.selectedMedia
                if (clipIndex !in clips.indices) return
                val globalOffset = clips.take(clipIndex).fold(0L) { acc, c -> acc + c.effectiveDurationMs }
                val globalPos = globalOffset + event.positionMs
                _state.update {
                    it.copy(
                        playbackPositionMs = globalPos.coerceIn(0L, it.totalDurationMs),
                        currentClipLocalPositionMs = event.positionMs,
                    )
                }
            }

            is EditorContract.Event.PlayerClipFinished -> {
                photoTimerJob?.cancel()
                val clips = _state.value.selectedMedia
                val nextIndex = event.clipIndex + 1
                if (nextIndex < clips.size) {
                    val nextClip = clips[nextIndex]
                    _state.update {
                        it.copy(
                            currentClipIndex = nextIndex,
                            currentClipLocalPositionMs = 0L,
                        )
                    }
                    if (nextClip.isVideo) {
                        _effects.tryEmit(EditorContract.Effect.PreparePlayer(nextIndex, 0L))
                        if (_state.value.isPlaying) {
                            _effects.tryEmit(EditorContract.Effect.StartPlayer)
                        }
                    } else if (_state.value.isPlaying) {
                        startPhotoTimer(nextIndex)
                    }
                } else {
                    _state.update {
                        it.copy(
                            isPlaying = false,
                            playbackPositionMs = 0L,
                            currentClipIndex = 0,
                            currentClipLocalPositionMs = 0L,
                        )
                    }
                    _effects.tryEmit(EditorContract.Effect.PausePlayer)
                    _effects.tryEmit(EditorContract.Effect.PreparePlayer(0, 0L))
                }
            }

            is EditorContract.Event.ToolbarTabSelected -> {
                _state.update {
                    if (it.activeToolbarTab == event.tab) {
                        it.copy(activeToolbarTab = null, showCaptionsPanel = false, showAutoCaptionsSheet = false)
                    } else {
                        it.copy(
                            activeToolbarTab = event.tab,
                            showCaptionsPanel = event.tab == EditorContract.ToolbarTab.Captions,
                            showAutoCaptionsSheet = false,
                        )
                    }
                }
            }

            EditorContract.Event.DismissToolbarPanel -> {
                _state.update {
                    it.copy(activeToolbarTab = null, showCaptionsPanel = false, showAutoCaptionsSheet = false)
                }
            }

            EditorContract.Event.ShowCaptionsPanel -> {
                _state.update {
                    it.copy(showCaptionsPanel = true, activeToolbarTab = EditorContract.ToolbarTab.Captions)
                }
            }

            EditorContract.Event.DismissCaptionsPanel -> {
                _state.update { it.copy(showCaptionsPanel = false, showAutoCaptionsSheet = false) }
            }

            is EditorContract.Event.CaptionsOptionSelected -> {
                when (event.option) {
                    EditorContract.CaptionsOption.AutoCaptions ->
                        _state.update { it.copy(showAutoCaptionsSheet = true) }
                    EditorContract.CaptionsOption.AddCaptions -> addEmptyCaption()
                    else -> _effects.tryEmit(EditorContract.Effect.ShowMessage("${event.option.name} coming soon."))
                }
            }

            EditorContract.Event.ShowAutoCaptionsSheet ->
                _state.update { it.copy(showAutoCaptionsSheet = true) }

            EditorContract.Event.DismissAutoCaptionsSheet ->
                _state.update { it.copy(showAutoCaptionsSheet = false) }

            is EditorContract.Event.AutoCaptionsLanguageChanged ->
                _state.update { it.copy(autoCaptionsConfig = it.autoCaptionsConfig.copy(language = event.language)) }

            is EditorContract.Event.AutoCaptionsFillerWordsToggled ->
                _state.update { it.copy(autoCaptionsConfig = it.autoCaptionsConfig.copy(identifyFillerWords = event.enabled)) }

            is EditorContract.Event.AutoCaptionsSourceChanged ->
                _state.update { it.copy(autoCaptionsConfig = it.autoCaptionsConfig.copy(source = event.source)) }

            EditorContract.Event.GenerateAutoCaptions -> generateCaptions()

            is EditorContract.Event.DeleteCaption -> {
                _state.update { it.copy(captions = it.captions.filter { c -> c.id != event.captionId }) }
                scheduleAutosave()
            }

            is EditorContract.Event.EditCaption -> {
                _state.update {
                    it.copy(captions = it.captions.map { c ->
                        if (c.id == event.captionId) c.copy(text = event.newText) else c
                    })
                }
                scheduleAutosave()
            }

            is EditorContract.Event.AspectRatioSelected -> {
                _state.update { it.copy(selectedAspectRatio = event.ratio) }
                scheduleAutosave()
            }

            is EditorContract.Event.TextChanged -> {
                _state.update { current ->
                    current.copy(textFields = current.textFields.map { item ->
                        if (item.id == event.fieldId) item.copy(value = event.value.take(item.maxLength)) else item
                    })
                }
                scheduleAutosave()
            }

            is EditorContract.Event.TransitionSelected -> {
                _state.update { current ->
                    current.copy(transitions = current.transitions.map { it.copy(isSelected = it.preset == event.preset) })
                }
                scheduleAutosave()
            }

            is EditorContract.Event.TransitionDurationChanged -> {
                _state.update { it.copy(transitionDurationMs = event.durationMs.coerceIn(120, 1600)) }
                scheduleAutosave()
            }

            is EditorContract.Event.TransitionIntensityChanged -> {
                _state.update { it.copy(transitionIntensityPercent = event.intensityPercent.coerceIn(0, 100)) }
                scheduleAutosave()
            }

            is EditorContract.Event.ReorderMedia -> {
                reorderMedia(event.fromIndex, event.toIndex)
                scheduleAutosave()
            }

            is EditorContract.Event.TrimChanged -> {
                updateTrim(event.uri, event.startMs, event.endMs)
                scheduleAutosave()
            }

            is EditorContract.Event.MoveMediaUp -> {
                moveMedia(event.uri, -1)
                scheduleAutosave()
            }

            is EditorContract.Event.MoveMediaDown -> {
                moveMedia(event.uri, 1)
                scheduleAutosave()
            }

            is EditorContract.Event.ReplaceMedia -> {
                replaceMedia(event.currentUri, event.newUri)
            }

            is EditorContract.Event.DeleteMedia -> {
                deleteMedia(event.uri)
                scheduleAutosave()
            }

            EditorContract.Event.AddMediaClicked -> {
                _effects.tryEmit(EditorContract.Effect.RequestMediaPicker)
            }

            is EditorContract.Event.AdditionalMediaSelected -> {
                appendMedia(event.uris)
            }

            EditorContract.Event.AddAudioClicked -> {
                _effects.tryEmit(EditorContract.Effect.RequestAudioFilePicker)
            }

            is EditorContract.Event.AudioFileSelected -> {
                resolveAudioTrack(event.uri)
            }

            EditorContract.Event.RemoveAudioTrack -> {
                _state.update { it.copy(audioTrack = null) }
                scheduleAutosave()
            }

            is EditorContract.Event.AudioVolumeChanged -> {
                val track = _state.value.audioTrack ?: return
                _state.update {
                    it.copy(audioTrack = track.copy(volume = event.volume.coerceIn(0f, 1f)))
                }
                scheduleAutosave()
            }

            EditorContract.Event.ExportClicked -> handleExport()
        }
    }

    private fun startPhotoTimer(clipIndex: Int) {
        photoTimerJob?.cancel()
        val clips = _state.value.selectedMedia
        val clip = clips.getOrNull(clipIndex) ?: return
        if (clip.isVideo) return

        val durationMs = clip.effectiveDurationMs
        val tickInterval = 32L

        photoTimerJob = viewModelScope.launch {
            var elapsed = 0L
            while (isActive && elapsed < durationMs && _state.value.isPlaying) {
                delay(tickInterval)
                elapsed += tickInterval
                val globalOffset = clips.take(clipIndex).fold(0L) { acc, c -> acc + c.effectiveDurationMs }
                _state.update {
                    it.copy(
                        playbackPositionMs = (globalOffset + elapsed).coerceAtMost(it.totalDurationMs),
                        currentClipLocalPositionMs = elapsed.coerceAtMost(durationMs),
                    )
                }
            }
            if (isActive && _state.value.isPlaying) {
                onEvent(EditorContract.Event.PlayerClipFinished(clipIndex))
            }
        }
    }

    private fun resolveClipForGlobalPosition(globalMs: Long): Pair<Int, Long> {
        val clips = _state.value.selectedMedia
        if (clips.isEmpty()) return Pair(0, 0L)
        var cursor = 0L
        clips.forEachIndexed { index, clip ->
            val end = cursor + clip.effectiveDurationMs
            if (globalMs < end) return Pair(index, globalMs - cursor)
            cursor = end
        }
        return Pair(clips.lastIndex, clips.last().effectiveDurationMs)
    }

    private fun resolveMediaFromUris(uris: List<String>) {
        val distinctUris = uris.asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .take(20)
            .toList()

        if (distinctUris.isEmpty()) {
            _state.update {
                it.copy(isLoading = false, isResolvingMedia = false, errorMessage = "No media selected.")
            }
            return
        }

        _state.update { it.copy(isLoading = false, isResolvingMedia = true) }

        viewModelScope.launch(Dispatchers.IO) {
            val result = mediaAssetResolver.resolveAll(distinctUris)
            val items = result.assets.mapIndexed { index, asset ->
                asset.toUi(
                    order = index,
                    canMoveUp = index > 0,
                    canMoveDown = index < result.assets.lastIndex,
                )
            }

            _state.update {
                it.copy(
                    isResolvingMedia = false,
                    selectedMedia = items,
                    errorMessage = if (items.isEmpty()) "Selected media could not be read." else null,
                )
            }

            if (items.isNotEmpty()) {
                _effects.tryEmit(EditorContract.Effect.PreparePlayer(0, 0L))
                scheduleAutosave()
            }

            if (result.failures.isNotEmpty()) {
                _effects.tryEmit(EditorContract.Effect.ShowMessage("${result.failures.size} item(s) could not be read."))
            }
        }
    }

    private fun loadPersistedProject(projectId: ProjectId) {
        viewModelScope.launch {
            val session = projectSessionManager.getSession(projectId)
            if (session == null) {
                _state.update {
                    it.copy(isLoading = false, errorMessage = "Saved project could not be loaded.")
                }
                return@launch
            }

            currentProjectId = session.draft.id
            createdAtEpochMillis = session.draft.createdAtEpochMillis

            persistedAssetIdsByUri.clear()
            session.mediaAssets.forEach { asset ->
                persistedAssetIdsByUri[asset.sourceUri] = asset.id
            }

            val assetById = session.mediaAssets.associateBy { it.id.value }
            val selectedMedia = session.draft.slotBindings
                .sortedBy { it.order }
                .mapIndexedNotNull { index, binding ->
                    val asset = assetById[binding.mediaAssetId.value] ?: return@mapIndexedNotNull null
                    EditorContract.SelectedMediaItem(
                        uri = asset.sourceUri,
                        order = index,
                        isVideo = (asset.mimeType ?: "").startsWith("video/"),
                        fileName = asset.fileName ?: "Unnamed item",
                        durationLabel = asset.durationMs?.let { MediaDurationFormatter.format(it) },
                        resolutionLabel = if (asset.width != null && asset.height != null) "${asset.width}\u00D7${asset.height}" else null,
                        width = asset.width,
                        height = asset.height,
                        mimeType = asset.mimeType,
                        sourceDurationMs = asset.durationMs,
                        trimStartMs = binding.trimStartMs,
                        trimEndMs = binding.trimEndMs ?: asset.durationMs,
                        canTrim = (asset.mimeType ?: "").startsWith("video/") && (asset.durationMs ?: 0L) > 1500L,
                        canMoveUp = index > 0,
                        canMoveDown = index < session.draft.slotBindings.lastIndex,
                    )
                }

            val selectedTransition = session.draft.transitionOverrides.firstOrNull()?.transition
                ?: TransitionPreset.CUT

            val restoredAudioTrack = session.draft.audioSelection.let { audio ->
                val uri = audio.localUri
                if (audio.sourceKind == AudioSourceKind.LOCAL_URI && !uri.isNullOrBlank()) {
                    EditorContract.AudioTrackItem(
                        uri = uri,
                        fileName = uri.toUri().lastPathSegment ?: "Audio",
                        durationMs = audio.endMs ?: 0L,
                        trimStartMs = audio.startMs,
                        trimEndMs = audio.endMs,
                        volume = audio.volume,
                    )
                } else null
            }

            _state.update {
                it.copy(
                    isLoading = false,
                    isResolvingMedia = false,
                    projectName = session.draft.name,
                    selectedMedia = selectedMedia,
                    audioTrack = restoredAudioTrack,
                    transitions = enumValues<TransitionPreset>().map { preset ->
                        EditorContract.TransitionItem(
                            preset = preset,
                            label = preset.displayLabel(),
                            isSelected = preset == selectedTransition,
                        )
                    },
                    autosaveState = EditorContract.AutosaveState(),
                    errorMessage = null,
                )
            }

            if (selectedMedia.isNotEmpty()) {
                _effects.tryEmit(EditorContract.Effect.PreparePlayer(0, 0L))
            }

            projectSessionManager.setLastActive(projectId)
        }
    }

    private fun appendMedia(uris: List<String>) {
        val existingUris = _state.value.selectedMedia.map { it.uri }.toSet()
        val newUris = uris.filter { it !in existingUris }.take(20 - _state.value.selectedMedia.size)
        if (newUris.isEmpty()) return

        _state.update { it.copy(isResolvingMedia = true) }

        viewModelScope.launch(Dispatchers.IO) {
            val result = mediaAssetResolver.resolveAll(newUris)
            val currentItems = _state.value.selectedMedia
            val startIndex = currentItems.size
            val newItems = result.assets.mapIndexed { index, asset ->
                asset.toUi(
                    order = startIndex + index,
                    canMoveUp = true,
                    canMoveDown = false,
                )
            }

            _state.update { current ->
                current.copy(
                    isResolvingMedia = false,
                    selectedMedia = rebindOrders(current.selectedMedia + newItems),
                )
            }

            if (newItems.isNotEmpty()) {
                scheduleAutosave()
            }

            if (result.failures.isNotEmpty()) {
                _effects.tryEmit(EditorContract.Effect.ShowMessage("${result.failures.size} item(s) could not be read."))
            }
        }
    }

    private fun resolveAudioTrack(uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val retriever = android.media.MediaMetadataRetriever()
            try {
                retriever.setDataSource(applicationContext, uri.toUri())
                val durationStr = retriever.extractMetadata(
                    android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
                )
                val durationMs = durationStr?.toLongOrNull() ?: 0L
                val title = retriever.extractMetadata(
                    android.media.MediaMetadataRetriever.METADATA_KEY_TITLE
                )
                val displayName = title?.takeIf { it.isNotBlank() }
                    ?: uri.toUri().lastPathSegment
                    ?: "Audio"

                val audioTrack = EditorContract.AudioTrackItem(
                    uri = uri,
                    fileName = displayName,
                    durationMs = durationMs,
                )

                _state.update { it.copy(audioTrack = audioTrack) }
                scheduleAutosave()
            } catch (_: Throwable) {
                _effects.tryEmit(
                    EditorContract.Effect.ShowMessage("Audio could not be read.")
                )
            } finally {
                runCatching { retriever.release() }
            }
        }
    }

    private fun replaceMedia(currentUri: String, newUri: String) {
        val currentItems = _state.value.selectedMedia
        val targetIndex = currentItems.indexOfFirst { it.uri == currentUri }
        if (targetIndex == -1) return

        _state.update { it.copy(isResolvingMedia = true) }

        viewModelScope.launch(Dispatchers.IO) {
            val result = mediaAssetResolver.resolveAll(listOf(newUri))
            val asset = result.assets.firstOrNull()

            if (asset == null) {
                _state.update { it.copy(isResolvingMedia = false) }
                _effects.tryEmit(EditorContract.Effect.ShowMessage("Replacement media could not be read."))
                return@launch
            }

            persistedAssetIdsByUri.remove(currentUri)
            val replacement = asset.toUi(
                order = targetIndex,
                canMoveUp = targetIndex > 0,
                canMoveDown = targetIndex < currentItems.lastIndex,
            )

            _state.update { current ->
                val updated = current.selectedMedia.toMutableList()
                if (targetIndex !in updated.indices) {
                    current.copy(isResolvingMedia = false)
                } else {
                    updated[targetIndex] = replacement
                    current.copy(
                        isResolvingMedia = false,
                        selectedMedia = rebindOrders(updated),
                        errorMessage = null,
                    )
                }
            }
            scheduleAutosave()
        }
    }

    private fun deleteMedia(uri: String) {
        persistedAssetIdsByUri.remove(uri)
        _state.update { current ->
            current.copy(selectedMedia = rebindOrders(current.selectedMedia.filterNot { it.uri == uri }))
        }
    }

    private fun reorderMedia(fromIndex: Int, toIndex: Int) {
        _state.update { current ->
            if (fromIndex !in current.selectedMedia.indices) return@update current
            if (toIndex !in current.selectedMedia.indices) return@update current
            if (fromIndex == toIndex) return@update current
            val mutable = current.selectedMedia.toMutableList()
            mutable.add(toIndex, mutable.removeAt(fromIndex))
            current.copy(selectedMedia = rebindOrders(mutable))
        }
    }

    private fun updateTrim(uri: String, startMs: Long, endMs: Long) {
        _state.update { current ->
            current.copy(selectedMedia = current.selectedMedia.map { item ->
                if (item.uri != uri || !item.canTrim) item
                else {
                    val sourceDuration = item.sourceDurationMs ?: 0L
                    val safeStart = startMs.coerceIn(0L, sourceDuration)
                    val safeEnd = endMs.coerceIn(safeStart + 300L, sourceDuration)
                    item.copy(
                        trimStartMs = safeStart,
                        trimEndMs = safeEnd,
                        durationLabel = MediaDurationFormatter.format(safeEnd - safeStart),
                    )
                }
            })
        }
    }

    private fun moveMedia(uri: String, offset: Int) {
        _state.update { current ->
            val idx = current.selectedMedia.indexOfFirst { it.uri == uri }
            if (idx == -1) return@update current
            val target = idx + offset
            if (target !in current.selectedMedia.indices) return@update current
            val mutable = current.selectedMedia.toMutableList()
            mutable.add(target, mutable.removeAt(idx))
            current.copy(selectedMedia = rebindOrders(mutable))
        }
    }

    private fun rebindOrders(items: List<EditorContract.SelectedMediaItem>): List<EditorContract.SelectedMediaItem> {
        return items.mapIndexed { index, item ->
            item.copy(order = index, canMoveUp = index > 0, canMoveDown = index < items.lastIndex)
        }
    }

    private fun addEmptyCaption() {
        val positionMs = _state.value.playbackPositionMs
        val newCaption = EditorContract.CaptionItem(
            id = UUID.randomUUID().toString(),
            text = "",
            startMs = positionMs,
            endMs = (positionMs + 2_000L).coerceAtMost(_state.value.totalDurationMs),
        )
        _state.update { it.copy(captions = it.captions + newCaption) }
        scheduleAutosave()
    }

    private fun generateCaptions() {
        if (_state.value.autoCaptionsConfig.isGenerating) return

        _state.update { it.copy(autoCaptionsConfig = it.autoCaptionsConfig.copy(isGenerating = true, generationProgress = 0f)) }

        viewModelScope.launch {
            val totalDuration = _state.value.totalDurationMs
            if (totalDuration <= 0L) {
                _state.update { it.copy(autoCaptionsConfig = it.autoCaptionsConfig.copy(isGenerating = false)) }
                _effects.tryEmit(EditorContract.Effect.ShowMessage("No media to generate captions from."))
                return@launch
            }

            for (step in 1..10) {
                delay(300L)
                _state.update { it.copy(autoCaptionsConfig = it.autoCaptionsConfig.copy(generationProgress = step / 10f)) }
            }

            val segmentDuration = 3_000L
            val generatedCaptions = buildList {
                var cursor = 0L
                var index = 1
                while (cursor < totalDuration) {
                    val end = (cursor + segmentDuration).coerceAtMost(totalDuration)
                    add(EditorContract.CaptionItem(
                        id = UUID.randomUUID().toString(),
                        text = "Caption segment $index",
                        startMs = cursor,
                        endMs = end,
                    ))
                    cursor = end
                    index++
                }
            }

            _state.update {
                it.copy(
                    captions = generatedCaptions,
                    autoCaptionsConfig = it.autoCaptionsConfig.copy(isGenerating = false, generationProgress = 1f),
                    showAutoCaptionsSheet = false,
                    showCaptionsPanel = false,
                    activeToolbarTab = null,
                )
            }
            _effects.tryEmit(EditorContract.Effect.ShowMessage("${generatedCaptions.size} captions generated."))
            scheduleAutosave()
        }
    }

    private fun handleExport() {
        val currentState = _state.value
        if (currentState.selectedMedia.isEmpty()) {
            _effects.tryEmit(EditorContract.Effect.ShowMessage("No media selected for this project."))
            return
        }
        if (currentState.isResolvingMedia) {
            _effects.tryEmit(EditorContract.Effect.ShowMessage("Media is still loading."))
            return
        }

        val selectedTransition = currentState.transitions.firstOrNull { it.isSelected }?.preset ?: TransitionPreset.CUT

        viewModelScope.launch {
            runCatching { persistCurrentSession(markSavingState = true, throwOnFailure = true) }
                .onFailure { throwable ->
                    _effects.tryEmit(EditorContract.Effect.ShowMessage(throwable.message ?: "Save failed."))
                    return@launch
                }

            _effects.tryEmit(
                EditorContract.Effect.NavigateExport(
                    payload = EditorContract.ExportPayload(
                        projectId = currentProjectId?.value,
                        templateId = TemplateId("direct-media"),
                        mediaClips = currentState.selectedMedia.map { item ->
                            EditorContract.EditedMediaClip(uri = item.uri, trimStartMs = item.trimStartMs, trimEndMs = item.trimEndMs)
                        },
                        textValues = currentState.textFields.map { item ->
                            EditorContract.EditedTextValue(fieldId = item.id, value = item.value)
                        },
                        transition = selectedTransition,
                        transitionDurationMs = currentState.transitionDurationMs,
                        transitionIntensityPercent = currentState.transitionIntensityPercent,
                        aspectRatioLabel = currentState.selectedAspectRatio,
                        backgroundMusicUri = currentState.audioTrack?.uri,
                    )
                )
            )
        }
    }

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveStatusResetJob?.cancel()
        val current = _state.value
        if (current.isLoading || current.isResolvingMedia) return
        markAutosaveSaving()
        autosaveJob = viewModelScope.launch {
            delay(500L)
            persistCurrentSession(markSavingState = false)
        }
    }

    private suspend fun persistCurrentSession(
        markSavingState: Boolean = true,
        throwOnFailure: Boolean = false,
    ) {
        val current = _state.value
        if (current.isLoading || current.isResolvingMedia) return
        if (markSavingState) markAutosaveSaving()

        try {
            val now = System.currentTimeMillis()
            val projectId = currentProjectId ?: ProjectId(UUID.randomUUID().toString()).also { currentProjectId = it }
            val createdAt = createdAtEpochMillis ?: now
            createdAtEpochMillis = createdAt

            val mediaAssets = current.selectedMedia.map { item ->
                val resolvedId = persistedAssetIdsByUri[item.uri]
                    ?: MediaAssetId(UUID.randomUUID().toString()).also { persistedAssetIdsByUri[item.uri] = it }
                ProjectMediaAssetRef(
                    id = resolvedId,
                    sourceUri = item.uri,
                    fileName = item.fileName,
                    mimeType = item.mimeType,
                    width = item.width,
                    height = item.height,
                    durationMs = item.sourceDurationMs,
                )
            }

            val slotBindings = current.selectedMedia.mapIndexed { index, item ->
                val assetId = persistedAssetIdsByUri[item.uri] ?: return@mapIndexed null
                ProjectSlotBinding(
                    slotId = com.muratcangzm.model.id.SlotId("slot-$index"),
                    mediaAssetId = assetId,
                    order = index,
                    trimStartMs = item.trimStartMs,
                    trimEndMs = item.trimEndMs,
                )
            }.filterNotNull()

            val selectedTransition = current.transitions.firstOrNull { it.isSelected }?.preset
            val transitionOverrides = if (selectedTransition != null && slotBindings.isNotEmpty()) {
                listOf(ProjectTransitionOverride(slotId = slotBindings.first().slotId, transition = selectedTransition))
            } else emptyList()

            val status = if (slotBindings.isEmpty()) ProjectStatus.DRAFT else ProjectStatus.READY

            val draft = ProjectDraft(
                id = projectId,
                name = current.projectName,
                templateId = TemplateId("direct-media"),
                aspectRatio = AspectRatio.VERTICAL_9_16,
                slotBindings = slotBindings,
                textValues = current.textFields.map { ProjectTextValue(fieldId = it.id, value = it.value) },
                transitionOverrides = transitionOverrides,
                audioSelection = current.audioTrack?.let { track ->
                    ProjectAudioSelection(
                        sourceKind = AudioSourceKind.LOCAL_URI,
                        localUri = track.uri,
                        startMs = track.trimStartMs,
                        endMs = track.trimEndMs ?: track.durationMs,
                        volume = track.volume,
                    )
                } ?: ProjectAudioSelection(),
                coverMediaAssetId = mediaAssets.firstOrNull()?.id,
                status = status,
                createdAtEpochMillis = createdAt,
                updatedAtEpochMillis = now,
            )

            projectSessionManager.saveSession(
                session = ProjectEditorSession(draft = draft, mediaAssets = mediaAssets),
                setActive = true,
            )
            markAutosaveSaved()
        } catch (throwable: Throwable) {
            markAutosaveError(throwable.message ?: "Save failed.")
            if (throwOnFailure) throw throwable
        }
    }

    private fun markAutosaveSaving() {
        autosaveStatusResetJob?.cancel()
        _state.update { it.copy(autosaveState = EditorContract.AutosaveState(EditorContract.AutosaveState.Status.Saving, "Saving changes.")) }
    }

    private fun markAutosaveSaved() {
        autosaveStatusResetJob?.cancel()
        _state.update { it.copy(autosaveState = EditorContract.AutosaveState(EditorContract.AutosaveState.Status.Saved, "All changes saved")) }
        autosaveStatusResetJob = viewModelScope.launch {
            delay(1800L)
            if (_state.value.autosaveState.status == EditorContract.AutosaveState.Status.Saved) {
                _state.update { it.copy(autosaveState = EditorContract.AutosaveState(EditorContract.AutosaveState.Status.Idle, "Auto-save enabled")) }
            }
        }
    }

    private fun markAutosaveError(message: String) {
        autosaveStatusResetJob?.cancel()
        _state.update { it.copy(autosaveState = EditorContract.AutosaveState(EditorContract.AutosaveState.Status.Error, message)) }
    }

    override fun onCleared() {
        autosaveJob?.cancel()
        autosaveStatusResetJob?.cancel()
        photoTimerJob?.cancel()
        super.onCleared()
    }
}

private fun MediaAsset.toUi(
    order: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
): EditorContract.SelectedMediaItem =
    EditorContract.SelectedMediaItem(
        uri = uri,
        order = order,
        isVideo = type == MediaType.VIDEO,
        fileName = fileName ?: "Unnamed item",
        durationLabel = if (type == MediaType.VIDEO) MediaDurationFormatter.format(durationMs) else null,
        resolutionLabel = if (width != null && height != null) "${width}\u00D7${height}" else null,
        width = width,
        height = height,
        mimeType = mimeType,
        sourceDurationMs = durationMs,
        trimStartMs = if (type == MediaType.VIDEO) 0L else null,
        trimEndMs = if (type == MediaType.VIDEO) durationMs else null,
        canTrim = type == MediaType.VIDEO && (durationMs ?: 0L) > 1500L,
        canMoveUp = canMoveUp,
        canMoveDown = canMoveDown,
    )

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
