package com.muratcangzm.model.project

import com.muratcangzm.model.id.AudioTrackId
import com.muratcangzm.model.id.MediaAssetId
import com.muratcangzm.model.id.ProjectId
import com.muratcangzm.model.id.SlotId
import com.muratcangzm.model.id.TemplateId
import com.muratcangzm.model.id.TextFieldId
import com.muratcangzm.model.template.AspectRatio
import com.muratcangzm.model.template.TransitionPreset

enum class ProjectStatus {
    DRAFT,
    READY,
    EXPORTING,
    EXPORTED,
    FAILED,
}

data class ProjectTextValue(
    val fieldId: TextFieldId,
    val value: String,
)

data class ProjectSlotBinding(
    val slotId: SlotId,
    val mediaAssetId: MediaAssetId,
    val order: Int = 0,
    val trimStartMs: Long? = null,
    val trimEndMs: Long? = null,
) {
    init {
        require(order >= 0) { "order must be >= 0." }
        require(trimStartMs == null || trimStartMs >= 0L) {
            "trimStartMs cannot be negative."
        }
        require(trimEndMs == null || trimEndMs >= 0L) {
            "trimEndMs cannot be negative."
        }

        if (trimStartMs != null && trimEndMs != null) {
            require(trimEndMs >= trimStartMs) {
                "trimEndMs cannot be smaller than trimStartMs."
            }
        }
    }
}

enum class AudioSourceKind {
    NONE,
    LOCAL_URI,
    CATALOG_TRACK,
}

data class ProjectAudioSelection(
    val sourceKind: AudioSourceKind = AudioSourceKind.NONE,
    val audioTrackId: AudioTrackId? = null,
    val localUri: String? = null,
    val startMs: Long = 0L,
    val endMs: Long? = null,
    val volume: Float = 1f,
) {
    init {
        require(startMs >= 0L) { "startMs cannot be negative." }
        require(endMs == null || endMs >= 0L) { "endMs cannot be negative." }
        require(volume in 0f..1f) { "volume must be between 0f and 1f." }

        if (startMs != 0L && sourceKind == AudioSourceKind.NONE) {
            error("startMs cannot be used when sourceKind is NONE.")
        }

        if (endMs != null) {
            require(endMs >= startMs) {
                "endMs cannot be smaller than startMs."
            }
        }

        when (sourceKind) {
            AudioSourceKind.NONE -> {
                require(audioTrackId == null) { "audioTrackId must be null for NONE." }
                require(localUri == null) { "localUri must be null for NONE." }
            }

            AudioSourceKind.LOCAL_URI -> {
                require(!localUri.isNullOrBlank()) {
                    "localUri is required for LOCAL_URI."
                }
            }

            AudioSourceKind.CATALOG_TRACK -> {
                require(audioTrackId != null) {
                    "audioTrackId is required for CATALOG_TRACK."
                }
            }
        }
    }
}

data class ProjectTransitionOverride(
    val slotId: SlotId,
    val transition: TransitionPreset,
)

data class ProjectDraft(
    val id: ProjectId,
    val name: String,
    val templateId: TemplateId,
    val aspectRatio: AspectRatio,
    val slotBindings: List<ProjectSlotBinding>,
    val textValues: List<ProjectTextValue> = emptyList(),
    val transitionOverrides: List<ProjectTransitionOverride> = emptyList(),
    val audioSelection: ProjectAudioSelection = ProjectAudioSelection(),
    val coverMediaAssetId: MediaAssetId? = null,
    val status: ProjectStatus = ProjectStatus.DRAFT,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
) {
    init {
        require(name.isNotBlank()) { "Project name cannot be blank." }
        require(createdAtEpochMillis > 0L) { "createdAtEpochMillis must be > 0." }
        require(updatedAtEpochMillis >= createdAtEpochMillis) {
            "updatedAtEpochMillis cannot be before createdAtEpochMillis."
        }

        val slotIds = slotBindings.map { it.slotId.value }
        require(slotIds.distinct().size == slotIds.size) {
            "Project slot bindings must be unique per slot."
        }

        val textIds = textValues.map { it.fieldId.value }
        require(textIds.distinct().size == textIds.size) {
            "Project text values must be unique per field."
        }
    }

    val mediaCount: Int
        get() = slotBindings.size
}
