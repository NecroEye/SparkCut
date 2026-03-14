package com.muratcangzm.editor.ui

import androidx.compose.runtime.Immutable
import com.muratcangzm.model.id.TemplateId
import com.muratcangzm.model.id.TextFieldId
import com.muratcangzm.model.template.TransitionPreset
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface EditorContract {

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val isResolvingMedia: Boolean = false,
        val template: TemplateSummary? = null,
        val selectedMedia: List<SelectedMediaItem> = emptyList(),
        val textFields: List<TextFieldItem> = emptyList(),
        val transitions: List<TransitionItem> = emptyList(),
        val errorMessage: String? = null,
    ) {
        val hasMissingRequiredFields: Boolean
            get() = textFields.any { it.required && it.value.isBlank() }

        val canExport: Boolean
            get() = template != null &&
                    selectedMedia.isNotEmpty() &&
                    !isResolvingMedia &&
                    !hasMissingRequiredFields &&
                    errorMessage == null
    }

    @Immutable
    data class TemplateSummary(
        val id: TemplateId,
        val name: String,
        val description: String,
        val categoryLabel: String,
        val aspectRatioLabel: String,
    )

    @Immutable
    data class SelectedMediaItem(
        val uri: String,
        val order: Int,
        val slotLabel: String,
        val isVideo: Boolean,
        val typeLabel: String,
        val fileName: String,
        val durationLabel: String?,
        val resolutionLabel: String?,
        val mimeType: String?,
        val sourceDurationMs: Long?,
        val trimStartMs: Long?,
        val trimEndMs: Long?,
        val canTrim: Boolean,
        val canMoveUp: Boolean,
        val canMoveDown: Boolean,
    )

    @Immutable
    data class EditedMediaClip(
        val uri: String,
        val trimStartMs: Long?,
        val trimEndMs: Long?,
    )

    @Immutable
    data class TextFieldItem(
        val id: TextFieldId,
        val label: String,
        val placeholder: String,
        val value: String,
        val maxLength: Int,
        val required: Boolean,
    )

    @Immutable
    data class TransitionItem(
        val preset: TransitionPreset,
        val label: String,
        val isSelected: Boolean,
    )

    @Immutable
    data class EditedTextValue(
        val fieldId: TextFieldId,
        val value: String,
    )

    @Immutable
    data class ExportPayload(
        val projectId: String?,
        val templateId: TemplateId,
        val mediaClips: List<EditedMediaClip>,
        val textValues: List<EditedTextValue>,
        val transition: TransitionPreset,
        val aspectRatioLabel: String,
    )

    sealed interface Event {
        data object BackClicked : Event

        data class TextChanged(
            val fieldId: TextFieldId,
            val value: String,
        ) : Event

        data class TransitionSelected(
            val preset: TransitionPreset,
        ) : Event

        data class MoveMediaUp(
            val uri: String,
        ) : Event

        data class MoveMediaDown(
            val uri: String,
        ) : Event

        data class ReorderMedia(
            val fromIndex: Int,
            val toIndex: Int,
        ) : Event

        data class TrimChanged(
            val uri: String,
            val startMs: Long,
            val endMs: Long,
        ) : Event

        data object ExportClicked : Event
    }

    sealed interface Effect {
        data object NavigateBack : Effect
        data class NavigateExport(val payload: ExportPayload) : Effect
        data class ShowMessage(val message: String) : Effect
    }

    interface Presenter {
        val state: StateFlow<State>
        val effects: SharedFlow<Effect>
        fun onEvent(event: Event)
    }
}
