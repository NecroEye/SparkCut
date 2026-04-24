package com.muratcangzm.export.ui

import androidx.compose.runtime.Immutable
import com.muratcangzm.model.export.ExportFps
import com.muratcangzm.model.export.ExportResolution
import com.muratcangzm.model.id.TemplateId
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface ExportContract {

    @Immutable
    data class MediaClipArg(
        val uri: String,
        val trimStartMs: Long?,
        val trimEndMs: Long?,
    )

    @Immutable
    data class AudioTrackArg(
        val uri: String,
        val fileName: String,
        val durationMs: Long,
        val trimStartMs: Long,
        val trimEndMs: Long?,
        val volume: Float,
        val offsetMs: Long,
    )

    @Immutable
    data class CaptionArg(
        val id: String,
        val text: String,
        val startMs: Long,
        val endMs: Long,
    )

    @Immutable
    data class TextOverlayArg(
        val id: String,
        val text: String,
        val startMs: Long,
        val endMs: Long,
        val gravity: String,
        val textSizeSp: Float,
    )

    @Immutable
    data class LaunchArgs(
        val projectId: String? = null,
        val templateId: String,
        val mediaClips: List<MediaClipArg>,
        val textValues: List<TextValueArg>,
        val transitionPresetName: String,
        val aspectRatioLabel: String,
        val backgroundMusicUri: String? = null,
        val isMuted: Boolean = false,
        val audioTracks: List<AudioTrackArg> = emptyList(),
        val resolutionWidth: Int = 1080,
        val resolutionHeight: Int = 1920,
        val captions: List<CaptionArg> = emptyList(),
        val textOverlays: List<TextOverlayArg> = emptyList(),
    )

    @Immutable
    data class TextValueArg(
        val fieldId: String,
        val value: String,
    )

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val template: TemplateSummary? = null,
        val mediaCount: Int = 0,
        val textValueCount: Int = 0,
        val transitionLabel: String = "",
        val presets: List<PresetItem> = emptyList(),
        val selectedPresetId: String? = null,
        val backgroundMusicUri: String? = null,
        val clipAudioVolume: Float = 1f,
        val musicAudioVolume: Float = 0.65f,
        val fadeInMs: Long = 600L,
        val fadeOutMs: Long = 600L,
        val isExporting: Boolean = false,
        val isPublishing: Boolean = false,
        val progress: Float = 0f,
        val soundtrackDisplayName: String? = null,
        val soundtrackDurationLabel: String? = null,
        val soundtrackMimeType: String? = null,
        val soundtrackErrorMessage: String? = null,
        val isLoadingSoundtrackMetadata: Boolean = false,
        val statusText: String = "",
        val completedOutputPath: String? = null,
        val publishedMediaUri: String? = null,
        val errorMessage: String? = null,
    ) {
        val canStartExport: Boolean
            get() = !isLoading &&
                    !isExporting &&
                    !isPublishing &&
                    !isLoadingSoundtrackMetadata &&
                    template != null &&
                    mediaCount > 0 &&
                    selectedPresetId != null &&
                    errorMessage == null &&
                    soundtrackErrorMessage == null

        val canSaveToGallery: Boolean
            get() = !isExporting &&
                    !isPublishing &&
                    !completedOutputPath.isNullOrBlank() &&
                    publishedMediaUri == null

        val canShare: Boolean
            get() = !publishedMediaUri.isNullOrBlank()

        val hasBackgroundMusic: Boolean
            get() = !backgroundMusicUri.isNullOrBlank()

        val progressPercent: Int
            get() = (progress * 100).toInt().coerceIn(0, 100)
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
    data class PresetItem(
        val id: String,
        val label: String,
        val detail: String,
        val resolution: ExportResolution,
        val fps: ExportFps,
        val isSelected: Boolean,
    )

    sealed interface Event {
        data object BackClicked : Event
        data class PresetSelected(val presetId: String) : Event
        data object StartExportClicked : Event
        data object SaveToGalleryClicked : Event
        data object ShareClicked : Event

        data object PickMusicClicked : Event
        data class MusicPicked(val uri: String?) : Event
        data object ClearMusicClicked : Event

        data class ClipAudioVolumeChanged(val value: Float) : Event
        data class MusicAudioVolumeChanged(val value: Float) : Event
        data class FadeInChanged(val valueMs: Long) : Event
        data class FadeOutChanged(val valueMs: Long) : Event

        data class ProgressUpdated(val progress: Float) : Event
        data object FakeExportCompleted : Event
    }

    sealed interface Effect {
        data object NavigateBack : Effect
        data object OpenAudioPicker : Effect
        data class ShareMedia(val contentUri: String) : Effect
        data class ShowMessage(val message: String) : Effect
    }

    interface Presenter {
        val state: StateFlow<State>
        val effects: SharedFlow<Effect>
        fun onEvent(event: Event)
    }
}
