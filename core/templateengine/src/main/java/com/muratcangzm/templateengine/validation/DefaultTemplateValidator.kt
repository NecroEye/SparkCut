package com.muratcangzm.templateengine.validation

import com.muratcangzm.model.template.TemplateCoverSource
import com.muratcangzm.model.template.TemplateSpec
import com.muratcangzm.model.template.TemplateTimingStrategy

class DefaultTemplateValidator : TemplateValidator {

    override fun validate(template: TemplateSpec): TemplateValidationResult {
        val issues = buildList {
            val sortedIndices = template.slots.map { it.index }.sorted()
            val expectedIndices = (0 until template.slots.size).toList()
            if (sortedIndices != expectedIndices) {
                add(
                    TemplateValidationIssue(
                        code = "NON_SEQUENTIAL_SLOT_INDICES",
                        severity = ValidationSeverity.ERROR,
                        message = "Template slot indices must be sequential from 0 to slotCount - 1.",
                    ),
                )
            }

            if (template.slots.none { it.isHero }) {
                add(
                    TemplateValidationIssue(
                        code = "MISSING_HERO_SLOT",
                        severity = ValidationSeverity.WARNING,
                        message = "Template does not define a hero slot.",
                    ),
                )
            }

            if (
                template.preview.coverSource == TemplateCoverSource.CUSTOM_SLOT &&
                template.preview.coverSlotId != null &&
                template.slots.none { it.id == template.preview.coverSlotId }
            ) {
                add(
                    TemplateValidationIssue(
                        code = "INVALID_PREVIEW_COVER_SLOT",
                        severity = ValidationSeverity.ERROR,
                        message = "Preview cover slot does not exist in template slots.",
                    ),
                )
            }

            val normalizedTags = template.tags.map { it.lowercase() }
            if (normalizedTags.distinct().size != normalizedTags.size) {
                add(
                    TemplateValidationIssue(
                        code = "DUPLICATE_TAGS",
                        severity = ValidationSeverity.WARNING,
                        message = "Template contains duplicate tags when compared case-insensitively.",
                    ),
                )
            }

            val timing = template.timingStrategy
            if (
                timing is TemplateTimingStrategy.AudioDriven &&
                template.musicPolicy.selectionPolicy.name == "OPTIONAL"
            ) {
                add(
                    TemplateValidationIssue(
                        code = "AUDIO_DRIVEN_OPTIONAL_MUSIC",
                        severity = ValidationSeverity.WARNING,
                        message = "Audio-driven timing usually works better when music is recommended or required.",
                    ),
                )
            }

            if (template.minMediaCount == template.maxMediaCount && template.slots.size > template.maxMediaCount) {
                add(
                    TemplateValidationIssue(
                        code = "UNUSED_TEMPLATE_SLOTS",
                        severity = ValidationSeverity.WARNING,
                        message = "Template has more slots than its declared maximum media count.",
                    ),
                )
            }
        }

        return TemplateValidationResult(
            templateId = template.id,
            issues = issues,
        )
    }
}