package com.muratcangzm.create.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muratcangzm.media.domain.MediaAssetResolver
import com.muratcangzm.media.domain.MediaDurationFormatter
import com.muratcangzm.model.id.TemplateId
import com.muratcangzm.model.media.MediaAsset
import com.muratcangzm.model.media.MediaType
import com.muratcangzm.model.template.AspectRatio
import com.muratcangzm.model.template.TemplateCategory
import com.muratcangzm.model.template.TemplateSpec
import com.muratcangzm.templateengine.catalog.TemplateCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreateViewModel(
    private val templateCatalog: TemplateCatalog,
    private val mediaAssetResolver: MediaAssetResolver,
    templateIdArg: String,
) : ViewModel(), CreateContract.Presenter {

    private val _state = MutableStateFlow(CreateContract.State())
    override val state: StateFlow<CreateContract.State> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<CreateContract.Effect>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val effects: SharedFlow<CreateContract.Effect> = _effects.asSharedFlow()

    private var loadedTemplate: TemplateSpec? = null

    init {
        loadTemplate(templateIdArg = templateIdArg)
    }

    override fun onEvent(event: CreateContract.Event) {
        when (event) {
            CreateContract.Event.BackClicked -> {
                _effects.tryEmit(CreateContract.Effect.NavigateBack)
            }

            CreateContract.Event.AddMediaClicked -> {
                val currentState = _state.value
                if (!currentState.canAddMore) {
                    _effects.tryEmit(
                        CreateContract.Effect.ShowMessage(
                            "You already reached the template media limit."
                        )
                    )
                    return
                }

                _effects.tryEmit(
                    CreateContract.Effect.OpenMediaPicker(
                        maxItems = currentState.remainingSelectableCount,
                    )
                )
            }

            is CreateContract.Event.MediaPicked -> {
                consumePickedMedia(event.uris)
            }

            is CreateContract.Event.RemoveMediaClicked -> {
                removeMedia(event.uri)
            }

            CreateContract.Event.ContinueClicked -> {
                val currentState = _state.value
                val template = loadedTemplate

                if (template == null) {
                    _effects.tryEmit(
                        CreateContract.Effect.ShowMessage("Template could not be loaded.")
                    )
                    return
                }

                if (!currentState.canContinue) {
                    _effects.tryEmit(
                        CreateContract.Effect.ShowMessage(
                            "Select at least ${currentState.minRequiredCount} items to continue."
                        )
                    )
                    return
                }

                _effects.tryEmit(
                    CreateContract.Effect.NavigateEditor(
                        templateId = template.id,
                        mediaUris = currentState.selectedMedia.map { it.uri },
                    )
                )
            }
        }
    }

    private fun loadTemplate(templateIdArg: String) {
        val templateId = runCatching { TemplateId(templateIdArg) }.getOrNull()
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

        _state.update {
            it.copy(
                isLoading = false,
                template = template.toSummary(),
                minRequiredCount = template.minMediaCount,
                maxAllowedCount = template.maxMediaCount,
                errorMessage = null,
            )
        }
    }

    private fun consumePickedMedia(uris: List<String>) {
        if (uris.isEmpty()) return

        val currentState = _state.value
        val existingUris = currentState.selectedMedia.map { it.uri }.toSet()
        val pendingUris = uris
            .asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .filterNot(existingUris::contains)
            .take(currentState.remainingSelectableCount)
            .toList()

        if (pendingUris.isEmpty()) {
            return
        }

        _state.update { it.copy(isResolvingMedia = true) }

        viewModelScope.launch(Dispatchers.IO) {
            val result = mediaAssetResolver.resolveAll(pendingUris)

            val appendedItems = result.assets.mapIndexed { index, asset ->
                asset.toUi(
                    order = currentState.selectedMedia.size + index,
                )
            }

            _state.update { previous ->
                val merged = (previous.selectedMedia + appendedItems)
                    .take(previous.maxAllowedCount)
                    .mapIndexed { index, item ->
                        item.copy(order = index)
                    }

                previous.copy(
                    isResolvingMedia = false,
                    selectedMedia = merged,
                )
            }

            if (result.failures.isNotEmpty()) {
                _effects.tryEmit(
                    CreateContract.Effect.ShowMessage(
                        "${result.failures.size} item(s) could not be read."
                    )
                )
            }
        }
    }

    private fun removeMedia(uri: String) {
        _state.update { current ->
            val updated = current.selectedMedia
                .filterNot { it.uri == uri }
                .mapIndexed { index, item ->
                    item.copy(order = index)
                }

            current.copy(selectedMedia = updated)
        }
    }
}

private fun TemplateSpec.toSummary(): CreateContract.TemplateSummary =
    CreateContract.TemplateSummary(
        id = id,
        name = name,
        description = description,
        categoryLabel = category.displayLabel(),
        aspectRatioLabel = aspectRatio.displayLabel(),
        minMediaCount = minMediaCount,
        maxMediaCount = maxMediaCount,
    )

private fun MediaAsset.toUi(
    order: Int,
): CreateContract.SelectedMediaItem =
    CreateContract.SelectedMediaItem(
        uri = uri,
        order = order,
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
        mimeType = mimeType,
    )

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