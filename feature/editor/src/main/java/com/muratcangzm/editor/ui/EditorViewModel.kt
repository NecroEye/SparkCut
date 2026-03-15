package com.muratcangzm.editor.ui

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
import com.muratcangzm.model.project.ProjectAudioSelection
import com.muratcangzm.model.project.ProjectDraft
import com.muratcangzm.model.project.ProjectEditorSession
import com.muratcangzm.model.project.ProjectMediaAssetRef
import com.muratcangzm.model.project.ProjectSlotBinding
import com.muratcangzm.model.project.ProjectStatus
import com.muratcangzm.model.project.ProjectTextValue
import com.muratcangzm.model.project.ProjectTransitionOverride
import com.muratcangzm.model.template.AspectRatio
import com.muratcangzm.model.template.TemplateCategory
import com.muratcangzm.model.template.TemplateSpec
import com.muratcangzm.model.template.TransitionPreset
import com.muratcangzm.templateengine.catalog.TemplateCatalog
import com.muratcangzm.templateengine.render.RenderValidationEngine
import com.muratcangzm.templateengine.render.RenderValidationResult
import com.muratcangzm.templateengine.render.RenderValidationSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
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
    private val templateCatalog: TemplateCatalog,
    private val mediaAssetResolver: MediaAssetResolver,
    private val templateIdArg: String?,
    private val mediaUrisArg: List<String>,
    private val projectSessionManager: ProjectSessionManager,
    private val projectIdArg: String?,
    private val renderValidationEngine: RenderValidationEngine,
) : ViewModel(), EditorContract.Presenter {

    private val _state = MutableStateFlow(EditorContract.State())
    override val state: StateFlow<EditorContract.State> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<EditorContract.Effect>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val effects: SharedFlow<EditorContract.Effect> = _effects.asSharedFlow()

    private var loadedTemplate: TemplateSpec? = null
    private var currentProjectId: ProjectId? =
        projectIdArg?.takeIf { it.isNotBlank() }?.let(::ProjectId)
    private var createdAtEpochMillis: Long? = null
    private var currentProjectName: String? = null
    private val persistedAssetIdsByUri = linkedMapOf<String, MediaAssetId>()
    private var autosaveJob: Job? = null
    private var autosaveStatusResetJob: Job? = null

    init {
        if (currentProjectId != null) {
            loadPersistedProject(currentProjectId!!)
        } else {
            loadTemplate(
                templateIdArg = templateIdArg,
                mediaUrisArg = mediaUrisArg,
            )
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

            is EditorContract.Event.TextChanged -> {
                _state.update { current ->
                    current.copy(
                        textFields = current.textFields.map { item ->
                            if (item.id == event.fieldId) {
                                item.copy(value = event.value.take(item.maxLength))
                            } else {
                                item
                            }
                        }
                    )
                }
                scheduleAutosave()
            }

            is EditorContract.Event.ReorderMedia -> {
                reorderMedia(
                    fromIndex = event.fromIndex,
                    toIndex = event.toIndex,
                )
                scheduleAutosave()
            }

            is EditorContract.Event.TransitionSelected -> {
                _state.update { current ->
                    current.copy(
                        transitions = current.transitions.map { item ->
                            item.copy(isSelected = item.preset == event.preset)
                        }
                    )
                }
                scheduleAutosave()
            }

            is EditorContract.Event.TrimChanged -> {
                updateTrim(
                    uri = event.uri,
                    startMs = event.startMs,
                    endMs = event.endMs,
                )
                scheduleAutosave()
            }

            is EditorContract.Event.MoveMediaUp -> {
                moveMedia(uri = event.uri, offset = -1)
                scheduleAutosave()
            }

            is EditorContract.Event.MoveMediaDown -> {
                moveMedia(uri = event.uri, offset = 1)
                scheduleAutosave()
            }

            EditorContract.Event.ExportClicked -> {
                val currentState = _state.value
                val template = loadedTemplate

                if (template == null) {
                    _effects.tryEmit(
                        EditorContract.Effect.ShowMessage("Template could not be loaded.")
                    )
                    return
                }

                if (currentState.selectedMedia.isEmpty()) {
                    _effects.tryEmit(
                        EditorContract.Effect.ShowMessage("No media selected for this project.")
                    )
                    return
                }

                if (currentState.isResolvingMedia) {
                    _effects.tryEmit(
                        EditorContract.Effect.ShowMessage("Media is still loading.")
                    )
                    return
                }

                val missingRequired = currentState.textFields
                    .filter { it.required && it.value.isBlank() }

                if (missingRequired.isNotEmpty()) {
                    _effects.tryEmit(
                        EditorContract.Effect.ShowMessage(
                            "Please fill all required text fields before export."
                        )
                    )
                    return
                }

                val selectedTransition = currentState.transitions
                    .firstOrNull { it.isSelected }
                    ?.preset
                    ?: template.defaultTransition

                viewModelScope.launch {
                    val validationResult = runCatching {
                        persistCurrentSession(
                            markSavingState = true,
                            throwOnFailure = true,
                        )
                    }.getOrElse { throwable ->
                        _effects.tryEmit(
                            EditorContract.Effect.ShowMessage(
                                throwable.message ?: "Project could not be saved before export."
                            )
                        )
                        return@launch
                    }

                    if (validationResult?.hasErrors == true) {
                        _effects.tryEmit(
                            EditorContract.Effect.ShowMessage(
                                validationResult.issues
                                    .firstOrNull { it.severity == RenderValidationSeverity.ERROR }
                                    ?.message
                                    ?: "Project validation failed."
                            )
                        )
                        return@launch
                    }

                    _effects.tryEmit(
                        EditorContract.Effect.NavigateExport(
                            payload = EditorContract.ExportPayload(
                                projectId = currentProjectId?.value,
                                templateId = template.id,
                                mediaClips = currentState.selectedMedia.map { item ->
                                    EditorContract.EditedMediaClip(
                                        uri = item.uri,
                                        trimStartMs = item.trimStartMs,
                                        trimEndMs = item.trimEndMs,
                                    )
                                },
                                textValues = currentState.textFields.map { item ->
                                    EditorContract.EditedTextValue(
                                        fieldId = item.id,
                                        value = item.value,
                                    )
                                },
                                transition = selectedTransition,
                                aspectRatioLabel = currentState.template?.aspectRatioLabel.orEmpty(),
                            )
                        )
                    )
                }
            }
        }
    }

    private fun loadTemplate(
        templateIdArg: String?,
        mediaUrisArg: List<String>,
    ) {
        val normalizedTemplateId = templateIdArg?.trim().orEmpty()
        val templateId = runCatching { TemplateId(normalizedTemplateId) }.getOrNull()
        if (templateId == null) {
            _state.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Invalid template id.",
                    validationErrors = emptyList(),
                    validationWarnings = emptyList(),
                    autosaveState = EditorContract.AutosaveState(),
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
                    validationErrors = emptyList(),
                    validationWarnings = emptyList(),
                    autosaveState = EditorContract.AutosaveState(),
                )
            }
            return
        }

        loadedTemplate = template

        val textFields = template.textFields.map { field ->
            EditorContract.TextFieldItem(
                id = field.id,
                label = field.label,
                placeholder = field.placeholder,
                value = field.defaultValue,
                maxLength = field.maxLength,
                required = field.required,
            )
        }

        val transitions = enumValues<TransitionPreset>().map { preset ->
            EditorContract.TransitionItem(
                preset = preset,
                label = preset.displayLabel(),
                isSelected = preset == template.defaultTransition,
            )
        }

        _state.update {
            it.copy(
                isLoading = false,
                isResolvingMedia = true,
                template = template.toSummary(),
                textFields = textFields,
                transitions = transitions,
                validationErrors = emptyList(),
                validationWarnings = emptyList(),
                autosaveState = EditorContract.AutosaveState(),
                errorMessage = null,
            )
        }

        resolveMedia(mediaUrisArg, template)
    }

    private fun loadPersistedProject(projectId: ProjectId) {
        viewModelScope.launch {
            val session = projectSessionManager.getSession(projectId)
            if (session == null) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Saved project could not be loaded.",
                        validationErrors = emptyList(),
                        validationWarnings = emptyList(),
                        autosaveState = EditorContract.AutosaveState(),
                    )
                }
                return@launch
            }

            val template = templateCatalog.getById(session.draft.templateId)
            if (template == null) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Template for saved project no longer exists.",
                        validationErrors = emptyList(),
                        validationWarnings = emptyList(),
                        autosaveState = EditorContract.AutosaveState(),
                    )
                }
                return@launch
            }

            loadedTemplate = template
            currentProjectId = session.draft.id
            createdAtEpochMillis = session.draft.createdAtEpochMillis
            currentProjectName = session.draft.name

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
                        slotLabel = slotLabelFor(template, index),
                        isVideo = (asset.mimeType ?: "").startsWith("video/"),
                        typeLabel = if ((asset.mimeType ?: "").startsWith("video/")) "Video" else "Photo",
                        fileName = asset.fileName ?: "Unnamed item",
                        durationLabel = asset.durationMs?.let { MediaDurationFormatter.format(it) },
                        resolutionLabel = if (asset.width != null && asset.height != null) {
                            "${asset.width}×${asset.height}"
                        } else {
                            null
                        },
                        width = asset.width,
                        height = asset.height,
                        mimeType = asset.mimeType,
                        sourceDurationMs = asset.durationMs,
                        trimStartMs = binding.trimStartMs,
                        trimEndMs = binding.trimEndMs ?: asset.durationMs,
                        canTrim = (asset.mimeType ?: "").startsWith("video/") &&
                                (asset.durationMs ?: 0L) > 1500L,
                        canMoveUp = index > 0,
                        canMoveDown = index < session.draft.slotBindings.lastIndex,
                    )
                }

            val textFields = template.textFields.map { field ->
                val persisted = session.draft.textValues.firstOrNull { it.fieldId == field.id }
                EditorContract.TextFieldItem(
                    id = field.id,
                    label = field.label,
                    placeholder = field.placeholder,
                    value = persisted?.value ?: field.defaultValue,
                    maxLength = field.maxLength,
                    required = field.required,
                )
            }

            val selectedTransition = session.draft.transitionOverrides.firstOrNull()?.transition
                ?: template.defaultTransition

            val transitions = enumValues<TransitionPreset>().map { preset ->
                EditorContract.TransitionItem(
                    preset = preset,
                    label = preset.displayLabel(),
                    isSelected = preset == selectedTransition,
                )
            }

            _state.update {
                it.copy(
                    isLoading = false,
                    isResolvingMedia = false,
                    template = template.toSummary(),
                    selectedMedia = selectedMedia,
                    textFields = textFields,
                    transitions = transitions,
                    autosaveState = EditorContract.AutosaveState(),
                    errorMessage = null,
                )
            }

            validateCurrentSession(session)
            projectSessionManager.setLastActive(projectId)
        }
    }

    private fun resolveMedia(
        mediaUrisArg: List<String>,
        template: TemplateSpec,
    ) {
        val distinctUris = mediaUrisArg
            .asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .take(template.maxMediaCount)
            .toList()

        if (distinctUris.isEmpty()) {
            _state.update {
                it.copy(
                    isResolvingMedia = false,
                    selectedMedia = emptyList(),
                    validationErrors = emptyList(),
                    validationWarnings = emptyList(),
                    autosaveState = EditorContract.AutosaveState(),
                    errorMessage = "No media received from create flow.",
                )
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val result = mediaAssetResolver.resolveAll(distinctUris)

            val items = result.assets
                .take(template.maxMediaCount)
                .mapIndexed { index, asset ->
                    asset.toUi(
                        order = index,
                        slotLabel = slotLabelFor(template, index),
                        canMoveUp = index > 0,
                        canMoveDown = index < result.assets.lastIndex &&
                                index < template.maxMediaCount - 1,
                    )
                }

            _state.update {
                it.copy(
                    isResolvingMedia = false,
                    selectedMedia = items,
                    errorMessage = when {
                        items.isEmpty() -> "Selected media could not be read."
                        else -> null
                    },
                )
            }

            if (items.isNotEmpty()) {
                scheduleAutosave()
            }

            if (result.failures.isNotEmpty()) {
                _effects.tryEmit(
                    EditorContract.Effect.ShowMessage(
                        "${result.failures.size} item(s) could not be read."
                    )
                )
            }
        }
    }

    private fun reorderMedia(
        fromIndex: Int,
        toIndex: Int,
    ) {
        val template = loadedTemplate ?: return

        _state.update { current ->
            if (fromIndex !in current.selectedMedia.indices) return@update current
            if (toIndex !in current.selectedMedia.indices) return@update current
            if (fromIndex == toIndex) return@update current

            val mutable = current.selectedMedia.toMutableList()
            val moved = mutable.removeAt(fromIndex)
            mutable.add(toIndex, moved)

            current.copy(
                selectedMedia = rebindSlotAssignments(
                    template = template,
                    items = mutable,
                )
            )
        }
    }

    private fun updateTrim(
        uri: String,
        startMs: Long,
        endMs: Long,
    ) {
        _state.update { current ->
            current.copy(
                selectedMedia = current.selectedMedia.map { item ->
                    if (item.uri != uri || !item.canTrim) {
                        item
                    } else {
                        val sourceDuration = item.sourceDurationMs ?: 0L
                        val safeStart = startMs.coerceIn(0L, sourceDuration)
                        val safeEnd = endMs.coerceIn(safeStart + 300L, sourceDuration)

                        item.copy(
                            trimStartMs = safeStart,
                            trimEndMs = safeEnd,
                            durationLabel = MediaDurationFormatter.format(safeEnd - safeStart),
                        )
                    }
                }
            )
        }
    }

    private fun moveMedia(
        uri: String,
        offset: Int,
    ) {
        val template = loadedTemplate ?: return

        _state.update { current ->
            val currentIndex = current.selectedMedia.indexOfFirst { it.uri == uri }
            if (currentIndex == -1) return@update current

            val targetIndex = currentIndex + offset
            if (targetIndex !in current.selectedMedia.indices) return@update current

            val mutable = current.selectedMedia.toMutableList()
            val moved = mutable.removeAt(currentIndex)
            mutable.add(targetIndex, moved)

            current.copy(
                selectedMedia = rebindSlotAssignments(
                    template = template,
                    items = mutable,
                )
            )
        }
    }

    private fun rebindSlotAssignments(
        template: TemplateSpec,
        items: List<EditorContract.SelectedMediaItem>,
    ): List<EditorContract.SelectedMediaItem> {
        return items.mapIndexed { index, item ->
            item.copy(
                order = index,
                slotLabel = slotLabelFor(template, index),
                canMoveUp = index > 0,
                canMoveDown = index < items.lastIndex,
            )
        }
    }

    private fun slotLabelFor(
        template: TemplateSpec,
        index: Int,
    ): String {
        val slot = template.slots
            .sortedBy { it.index }
            .getOrNull(index)

        return if (slot != null) {
            "Slot ${slot.index + 1}"
        } else {
            "Slot ${index + 1}"
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
    ): RenderValidationResult? {
        val template = loadedTemplate ?: return null
        val current = _state.value
        if (current.isLoading || current.isResolvingMedia) return null

        if (markSavingState) {
            markAutosaveSaving()
        }

        return try {
            val now = System.currentTimeMillis()
            val projectId = currentProjectId ?: ProjectId(UUID.randomUUID().toString()).also {
                currentProjectId = it
            }
            val createdAt = createdAtEpochMillis ?: now
            createdAtEpochMillis = createdAt

            val orderedSlots = template.slots.sortedBy { it.index }

            val mediaAssets = current.selectedMedia.map { item ->
                val existingId = persistedAssetIdsByUri[item.uri]
                val resolvedId = existingId ?: MediaAssetId(UUID.randomUUID().toString()).also {
                    persistedAssetIdsByUri[item.uri] = it
                }

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

            val slotBindings = current.selectedMedia.mapIndexedNotNull { index, item ->
                val slot = orderedSlots.getOrNull(index) ?: return@mapIndexedNotNull null
                val assetId = persistedAssetIdsByUri[item.uri] ?: return@mapIndexedNotNull null

                ProjectSlotBinding(
                    slotId = slot.id,
                    mediaAssetId = assetId,
                    order = index,
                    trimStartMs = item.trimStartMs,
                    trimEndMs = item.trimEndMs,
                )
            }

            val selectedTransition = current.transitions.firstOrNull { it.isSelected }?.preset
            val firstSlot = orderedSlots.firstOrNull()

            val transitionOverrides = if (selectedTransition != null && firstSlot != null) {
                listOf(
                    ProjectTransitionOverride(
                        slotId = firstSlot.id,
                        transition = selectedTransition,
                    )
                )
            } else {
                emptyList()
            }

            val hasMissingRequired = current.textFields.any { it.required && it.value.isBlank() }
            val status = when {
                slotBindings.isEmpty() -> ProjectStatus.DRAFT
                hasMissingRequired -> ProjectStatus.DRAFT
                else -> ProjectStatus.READY
            }

            val draft = ProjectDraft(
                id = projectId,
                name = currentProjectName ?: template.name,
                templateId = template.id,
                aspectRatio = template.aspectRatio,
                slotBindings = slotBindings,
                textValues = current.textFields.map {
                    ProjectTextValue(
                        fieldId = it.id,
                        value = it.value,
                    )
                },
                transitionOverrides = transitionOverrides,
                audioSelection = ProjectAudioSelection(),
                coverMediaAssetId = mediaAssets.firstOrNull()?.id,
                status = status,
                createdAtEpochMillis = createdAt,
                updatedAtEpochMillis = now,
            )

            val session = ProjectEditorSession(
                draft = draft,
                mediaAssets = mediaAssets,
            )

            projectSessionManager.saveSession(
                session = session,
                setActive = true,
            )

            val validationResult = validateCurrentSession(session)
            markAutosaveSaved()
            validationResult
        } catch (throwable: Throwable) {
            val message = throwable.message ?: "Project changes could not be saved."
            markAutosaveError(message)
            if (throwOnFailure) throw throwable
            null
        }
    }

    private fun validateCurrentSession(
        session: ProjectEditorSession,
    ): RenderValidationResult {
        val template = loadedTemplate ?: return RenderValidationResult(emptyList())

        val result = renderValidationEngine.validate(
            template = template,
            session = session,
        )

        _state.update {
            it.copy(
                validationErrors = result.issues
                    .filter { issue -> issue.severity == RenderValidationSeverity.ERROR }
                    .map { issue -> issue.message },
                validationWarnings = result.issues
                    .filter { issue -> issue.severity == RenderValidationSeverity.WARNING }
                    .map { issue -> issue.message },
            )
        }

        return result
    }

    private fun markAutosaveSaving() {
        autosaveStatusResetJob?.cancel()

        _state.update {
            it.copy(
                autosaveState = EditorContract.AutosaveState(
                    status = EditorContract.AutosaveState.Status.Saving,
                    message = "Saving changes..."
                )
            )
        }
    }

    private fun markAutosaveSaved() {
        autosaveStatusResetJob?.cancel()

        _state.update {
            it.copy(
                autosaveState = EditorContract.AutosaveState(
                    status = EditorContract.AutosaveState.Status.Saved,
                    message = "All changes saved"
                )
            )
        }

        autosaveStatusResetJob = viewModelScope.launch {
            delay(1800L)

            val currentStatus = _state.value.autosaveState.status
            if (currentStatus == EditorContract.AutosaveState.Status.Saved) {
                _state.update {
                    it.copy(
                        autosaveState = EditorContract.AutosaveState(
                            status = EditorContract.AutosaveState.Status.Idle,
                            message = "Auto-save is enabled"
                        )
                    )
                }
            }
        }
    }

    private fun markAutosaveError(message: String) {
        autosaveStatusResetJob?.cancel()

        _state.update {
            it.copy(
                autosaveState = EditorContract.AutosaveState(
                    status = EditorContract.AutosaveState.Status.Error,
                    message = message
                )
            )
        }
    }

    override fun onCleared() {
        autosaveJob?.cancel()
        autosaveStatusResetJob?.cancel()
        super.onCleared()
    }
}

private fun TemplateSpec.toSummary(): EditorContract.TemplateSummary =
    EditorContract.TemplateSummary(
        id = id,
        name = name,
        description = description,
        categoryLabel = category.displayLabel(),
        aspectRatioLabel = aspectRatio.displayLabel(),
    )

private fun MediaAsset.toUi(
    order: Int,
    slotLabel: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
): EditorContract.SelectedMediaItem =
    EditorContract.SelectedMediaItem(
        uri = uri,
        order = order,
        slotLabel = slotLabel,
        isVideo = type == MediaType.VIDEO,
        typeLabel = when (type) {
            MediaType.IMAGE -> "Photo"
            MediaType.VIDEO -> "Video"
        },
        fileName = fileName ?: "Unnamed item",
        durationLabel = if (type == MediaType.VIDEO) {
            MediaDurationFormatter.format(durationMs)
        } else {
            null
        },
        resolutionLabel = if (width != null && height != null) {
            "${width}×${height}"
        } else {
            null
        },
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