package com.muratcangzm.media.domain.export

import com.muratcangzm.model.export.ExportPreset
import com.muratcangzm.model.id.ExportJobId
import com.muratcangzm.model.media.MediaType
import com.muratcangzm.model.template.TransitionPreset

data class MediaExportSequenceItem(
    val uri: String,
    val mediaType: MediaType,
    val durationMs: Long,
    val trimStartMs: Long? = null,
    val trimEndMs: Long? = null,
)

data class MediaExportRequest(
    val jobId: ExportJobId,
    val sequenceItems: List<MediaExportSequenceItem>,
    val preset: ExportPreset,
    val textValues: Map<String, String> = emptyMap(),
    val transitionPreset: TransitionPreset? = null,
    val textOverlays: List<ExportTextOverlay> = emptyList(),
    val transitionWindows: List<MediaTransitionWindow> = emptyList(),
    val visualStyle: ExportVisualStyle? = null,
    val audioMix: AudioMixRequest = AudioMixRequest(
        preserveOriginalClipAudio = true,
    ),
    val outputFileBaseName: String,
    val outputWidth: Int? = null,
    val outputHeight: Int? = null,
)

sealed interface MediaExportState {
    data object Idle : MediaExportState

    data class Running(
        val progress: Float,
        val statusText: String,
        val outputFilePath: String,
    ) : MediaExportState

    data class Completed(
        val outputFilePath: String,
        val fileSizeBytes: Long?,
    ) : MediaExportState

    data class Failed(
        val message: String,
        val outputFilePath: String?,
    ) : MediaExportState

    data class Cancelled(
        val outputFilePath: String?,
    ) : MediaExportState
}