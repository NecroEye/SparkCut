package com.muratcangzm.editor.ui

import androidx.compose.runtime.Immutable
import com.muratcangzm.model.id.TemplateId
import com.muratcangzm.model.id.TextFieldId
import com.muratcangzm.model.template.TransitionPreset
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface EditorContract {

    enum class ToolbarTab {
        Text,
        Overlay,
        Effects,
        Captions,
        AspectRatio,
        Filters,
    }

    enum class CaptionsOption {
        AddCaptions,
        AutoCaptions,
        CaptionTemplates,
        AutoLyrics,
        ImportCaptions,
    }

    enum class CaptionSource {
        All,
        Voiceover,
        Video,
    }

    @Immutable
    data class AutoCaptionsConfig(
        val language: String = "English",
        val identifyFillerWords: Boolean = false,
        val source: CaptionSource = CaptionSource.Video,
        val isGenerating: Boolean = false,
        val generationProgress: Float = 0f,
    )

    @Immutable
    data class CaptionItem(
        val id: String,
        val text: String,
        val startMs: Long,
        val endMs: Long,
        val isFiller: Boolean = false,
    )

    @Immutable
    data class AutosaveState(
        val status: Status = Status.Idle,
        val message: String? = null,
    ) {
        enum class Status { Idle, Saving, Saved, Error }
    }

    @Immutable
    data class AudioTrackItem(
        val uri: String,
        val fileName: String,
        val durationMs: Long,
        val trimStartMs: Long = 0L,
        val trimEndMs: Long? = null,
        val volume: Float = 1f,
    ) {
        val effectiveDurationMs: Long
            get() = (trimEndMs ?: durationMs) - trimStartMs
    }

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val isResolvingMedia: Boolean = false,
        val projectName: String = "Untitled Project",
        val selectedMedia: List<SelectedMediaItem> = emptyList(),
        val audioTrack: AudioTrackItem? = null,
        val textFields: List<TextFieldItem> = emptyList(),
        val transitions: List<TransitionItem> = emptyList(),
        val transitionDurationMs: Int = 350,
        val transitionIntensityPercent: Int = 65,
        val validationErrors: List<String> = emptyList(),
        val validationWarnings: List<String> = emptyList(),
        val autosaveState: AutosaveState = AutosaveState(),
        val errorMessage: String? = null,
        val isPlaying: Boolean = false,
        val playbackPositionMs: Long = 0L,
        val currentClipIndex: Int = 0,
        val currentClipLocalPositionMs: Long = 0L,
        val isMuted: Boolean = false,
        val activeToolbarTab: ToolbarTab? = null,
        val showCaptionsPanel: Boolean = false,
        val showAutoCaptionsSheet: Boolean = false,
        val autoCaptionsConfig: AutoCaptionsConfig = AutoCaptionsConfig(),
        val captions: List<CaptionItem> = emptyList(),
        val selectedAspectRatio: String = "9:16",
        val availableAspectRatios: List<String> = listOf("9:16", "16:9", "1:1", "4:5", "4:3"),
    ) {
        val canExport: Boolean
            get() = selectedMedia.isNotEmpty() && !isResolvingMedia && errorMessage == null

        val totalDurationMs: Long
            get() = selectedMedia.fold(0L) { acc, item -> acc + item.effectiveDurationMs }
    }

    @Immutable
    data class SelectedMediaItem(
        val uri: String,
        val order: Int,
        val isVideo: Boolean,
        val fileName: String,
        val durationLabel: String?,
        val resolutionLabel: String?,
        val width: Int?,
        val height: Int?,
        val mimeType: String?,
        val sourceDurationMs: Long?,
        val trimStartMs: Long?,
        val trimEndMs: Long?,
        val canTrim: Boolean,
        val canMoveUp: Boolean,
        val canMoveDown: Boolean,
    ) {
        val effectiveDurationMs: Long
            get() = when {
                trimStartMs != null && trimEndMs != null ->
                    (trimEndMs - trimStartMs).coerceAtLeast(300L)
                sourceDurationMs != null && sourceDurationMs > 0L -> sourceDurationMs
                else -> 3_000L
            }
    }

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
        val transitionDurationMs: Int,
        val transitionIntensityPercent: Int,
        val aspectRatioLabel: String,
        val backgroundMusicUri: String? = null,
    )

    sealed interface Event {
        data object BackClicked : Event
        data object TogglePlayPause : Event
        data class SeekTo(val positionMs: Long) : Event
        data object SeekBack : Event
        data object SeekForward : Event
        data object ToggleMute : Event
        data class PlaybackPositionUpdate(val positionMs: Long) : Event
        data class PlayerClipFinished(val clipIndex: Int) : Event
        data class ToolbarTabSelected(val tab: ToolbarTab) : Event
        data object DismissToolbarPanel : Event
        data class TextChanged(val fieldId: TextFieldId, val value: String) : Event
        data class TransitionSelected(val preset: TransitionPreset) : Event
        data class TransitionDurationChanged(val durationMs: Int) : Event
        data class TransitionIntensityChanged(val intensityPercent: Int) : Event
        data class MoveMediaUp(val uri: String) : Event
        data class MoveMediaDown(val uri: String) : Event
        data class ReplaceMedia(val currentUri: String, val newUri: String) : Event
        data class DeleteMedia(val uri: String) : Event
        data class ReorderMedia(val fromIndex: Int, val toIndex: Int) : Event
        data class TrimChanged(val uri: String, val startMs: Long, val endMs: Long) : Event
        data object ShowCaptionsPanel : Event
        data object DismissCaptionsPanel : Event
        data class CaptionsOptionSelected(val option: CaptionsOption) : Event
        data object ShowAutoCaptionsSheet : Event
        data object DismissAutoCaptionsSheet : Event
        data class AutoCaptionsLanguageChanged(val language: String) : Event
        data class AutoCaptionsFillerWordsToggled(val enabled: Boolean) : Event
        data class AutoCaptionsSourceChanged(val source: CaptionSource) : Event
        data object GenerateAutoCaptions : Event
        data class DeleteCaption(val captionId: String) : Event
        data class EditCaption(val captionId: String, val newText: String) : Event
        data class AspectRatioSelected(val ratio: String) : Event
        data object AddMediaClicked : Event
        data class AdditionalMediaSelected(val uris: List<String>) : Event
        data object AddAudioClicked : Event
        data class AudioFileSelected(val uri: String) : Event
        data object RemoveAudioTrack : Event
        data class AudioVolumeChanged(val volume: Float) : Event
        data object ExportClicked : Event
    }

    sealed interface Effect {
        data object NavigateBack : Effect
        data class NavigateExport(val payload: ExportPayload) : Effect
        data class ShowMessage(val message: String) : Effect
        data class PreparePlayer(val clipIndex: Int, val seekToMs: Long) : Effect
        data object StartPlayer : Effect
        data object PausePlayer : Effect
        data object RequestMediaPicker : Effect
        data object RequestAudioFilePicker : Effect
    }

    interface Presenter {
        val state: StateFlow<State>
        val effects: SharedFlow<Effect>
        fun onEvent(event: Event)
    }
}
