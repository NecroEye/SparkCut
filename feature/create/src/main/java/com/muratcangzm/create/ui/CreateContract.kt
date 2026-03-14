package com.muratcangzm.create.ui

import androidx.compose.runtime.Immutable
import com.muratcangzm.model.id.TemplateId
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface CreateContract {

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val isResolvingMedia: Boolean = false,
        val template: TemplateSummary? = null,
        val selectedMedia: List<SelectedMediaItem> = emptyList(),
        val minRequiredCount: Int = 0,
        val maxAllowedCount: Int = 0,
        val errorMessage: String? = null,
    ) {
        val selectedCount: Int
            get() = selectedMedia.size

        val remainingSelectableCount: Int
            get() = (maxAllowedCount - selectedCount).coerceAtLeast(0)

        val canAddMore: Boolean
            get() = !isResolvingMedia && remainingSelectableCount > 0

        val canContinue: Boolean
            get() = !isResolvingMedia &&
                    template != null &&
                    selectedCount >= minRequiredCount

        val selectionSummary: String
            get() = "$selectedCount / $maxAllowedCount selected"
    }

    @Immutable
    data class TemplateSummary(
        val id: TemplateId,
        val name: String,
        val description: String,
        val categoryLabel: String,
        val aspectRatioLabel: String,
        val minMediaCount: Int,
        val maxMediaCount: Int,
    )

    @Immutable
    data class SelectedMediaItem(
        val uri: String,
        val order: Int,
        val typeLabel: String,
        val fileName: String,
        val isVideo: Boolean,
        val durationLabel: String?,
        val resolutionLabel: String?,
        val mimeType: String?,
    )

    sealed interface Event {
        data object BackClicked : Event
        data object AddMediaClicked : Event
        data class MediaPicked(val uris: List<String>) : Event
        data class RemoveMediaClicked(val uri: String) : Event
        data object ContinueClicked : Event
    }

    sealed interface Effect {
        data class OpenMediaPicker(val maxItems: Int) : Effect
        data class NavigateEditor(
            val templateId: TemplateId,
            val mediaUris: List<String>,
        ) : Effect

        data object NavigateBack : Effect
        data class ShowMessage(val message: String) : Effect
    }

    interface Presenter {
        val state: StateFlow<State>
        val effects: SharedFlow<Effect>
        fun onEvent(event: Event)
    }
}