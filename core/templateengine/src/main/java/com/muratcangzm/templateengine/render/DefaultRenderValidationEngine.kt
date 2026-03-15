package com.muratcangzm.templateengine.render

import com.muratcangzm.model.media.MediaType
import com.muratcangzm.model.project.AudioSourceKind
import com.muratcangzm.model.project.ProjectEditorSession
import com.muratcangzm.model.template.TemplateSpec

class DefaultRenderValidationEngine : RenderValidationEngine {

    override fun validate(
        template: TemplateSpec,
        session: ProjectEditorSession,
    ): RenderValidationResult {
        val issues = mutableListOf<RenderValidationIssue>()

        val draft = session.draft
        val assetsById = session.mediaAssets.associateBy { it.id.value }
        val slotBindings = draft.slotBindings.sortedBy { it.order }

        if (slotBindings.size < template.minMediaCount) {
            issues += RenderValidationIssue(
                code = RenderValidationCode.SLOT_COUNT_BELOW_MIN,
                severity = RenderValidationSeverity.ERROR,
                message = "At least ${template.minMediaCount} media item(s) are required.",
                field = "slotBindings",
            )
        }

        if (slotBindings.size > template.maxMediaCount) {
            issues += RenderValidationIssue(
                code = RenderValidationCode.SLOT_COUNT_ABOVE_MAX,
                severity = RenderValidationSeverity.ERROR,
                message = "At most ${template.maxMediaCount} media item(s) are allowed.",
                field = "slotBindings",
            )
        }

        val slotIds = slotBindings.map { it.slotId.value }
        if (slotIds.distinct().size != slotIds.size) {
            issues += RenderValidationIssue(
                code = RenderValidationCode.SLOT_BINDING_DUPLICATE_SLOT,
                severity = RenderValidationSeverity.ERROR,
                message = "Each slot must be bound only once.",
                field = "slotBindings",
            )
        }

        val orders = slotBindings.map { it.order }
        if (orders.distinct().size != orders.size) {
            issues += RenderValidationIssue(
                code = RenderValidationCode.SLOT_BINDING_DUPLICATE_ORDER,
                severity = RenderValidationSeverity.ERROR,
                message = "Each slot binding order must be unique.",
                field = "slotBindings",
            )
        }

        slotBindings.forEach { binding ->
            val asset = assetsById[binding.mediaAssetId.value]
            if (asset == null) {
                issues += RenderValidationIssue(
                    code = RenderValidationCode.SLOT_BINDING_REFERENCES_UNKNOWN_ASSET,
                    severity = RenderValidationSeverity.ERROR,
                    message = "Slot ${binding.slotId.value} references a missing media asset.",
                    field = "slot:${binding.slotId.value}",
                )
                return@forEach
            }

            val isVideo = (asset.mimeType ?: "").startsWith("video/")

            if (isVideo) {
                val sourceDuration = asset.durationMs
                val trimStart = binding.trimStartMs
                val trimEnd = binding.trimEndMs

                if (sourceDuration != null) {
                    if (trimStart != null && trimStart !in 0L..sourceDuration) {
                        issues += RenderValidationIssue(
                            code = RenderValidationCode.VIDEO_TRIM_OUT_OF_RANGE,
                            severity = RenderValidationSeverity.ERROR,
                            message = "Trim start is outside video duration for slot ${binding.slotId.value}.",
                            field = "trim:${binding.slotId.value}",
                        )
                    }

                    if (trimEnd != null && trimEnd !in 0L..sourceDuration) {
                        issues += RenderValidationIssue(
                            code = RenderValidationCode.VIDEO_TRIM_OUT_OF_RANGE,
                            severity = RenderValidationSeverity.ERROR,
                            message = "Trim end is outside video duration for slot ${binding.slotId.value}.",
                            field = "trim:${binding.slotId.value}",
                        )
                    }
                }

                if (trimStart != null && trimEnd != null && trimEnd <= trimStart) {
                    issues += RenderValidationIssue(
                        code = RenderValidationCode.VIDEO_TRIM_INVALID,
                        severity = RenderValidationSeverity.ERROR,
                        message = "Trim end must be greater than trim start for slot ${binding.slotId.value}.",
                        field = "trim:${binding.slotId.value}",
                    )
                }
            }
        }

        val textById = draft.textValues.associateBy { it.fieldId.value }

        template.textFields.forEach { field ->
            val value = textById[field.id.value]?.value.orEmpty()

            if (field.required && value.isBlank()) {
                issues += RenderValidationIssue(
                    code = RenderValidationCode.REQUIRED_TEXT_MISSING,
                    severity = RenderValidationSeverity.ERROR,
                    message = "\"${field.label}\" is required.",
                    field = "text:${field.id.value}",
                )
            }

            if (value.length > field.maxLength) {
                issues += RenderValidationIssue(
                    code = RenderValidationCode.TEXT_VALUE_TOO_LONG,
                    severity = RenderValidationSeverity.ERROR,
                    message = "\"${field.label}\" exceeds max length ${field.maxLength}.",
                    field = "text:${field.id.value}",
                )
            }
        }

        draft.coverMediaAssetId?.let { coverId ->
            if (assetsById[coverId.value] == null) {
                issues += RenderValidationIssue(
                    code = RenderValidationCode.COVER_ASSET_MISSING,
                    severity = RenderValidationSeverity.WARNING,
                    message = "Cover media asset is missing.",
                    field = "coverMediaAssetId",
                )
            }
        }

        val audio = draft.audioSelection
        when (audio.sourceKind) {
            AudioSourceKind.NONE -> Unit

            AudioSourceKind.LOCAL_URI -> {
                if (audio.localUri.isNullOrBlank()) {
                    issues += RenderValidationIssue(
                        code = RenderValidationCode.AUDIO_SELECTION_INVALID,
                        severity = RenderValidationSeverity.ERROR,
                        message = "Local soundtrack URI is missing.",
                        field = "audioSelection",
                    )
                }
            }

            AudioSourceKind.CATALOG_TRACK -> {
                if (audio.audioTrackId == null) {
                    issues += RenderValidationIssue(
                        code = RenderValidationCode.AUDIO_SELECTION_INVALID,
                        severity = RenderValidationSeverity.ERROR,
                        message = "Catalog soundtrack id is missing.",
                        field = "audioSelection",
                    )
                }
            }
        }

        return RenderValidationResult(issues = issues)
    }
}
