package com.muratcangzm.templateengine.parser

import com.muratcangzm.model.id.SlotId
import com.muratcangzm.model.id.TemplateId
import com.muratcangzm.model.id.TextFieldId
import com.muratcangzm.model.template.AspectRatio
import com.muratcangzm.model.template.AudioSelectionPolicy
import com.muratcangzm.model.template.AudioTrimBehavior
import com.muratcangzm.model.template.SlotAcceptedType
import com.muratcangzm.model.template.SlotFillMode
import com.muratcangzm.model.template.TemplateCategory
import com.muratcangzm.model.template.TemplateCoverSource
import com.muratcangzm.model.template.TemplateMusicPolicy
import com.muratcangzm.model.template.TemplatePreviewSpec
import com.muratcangzm.model.template.TemplateSlotSpec
import com.muratcangzm.model.template.TemplateSpec
import com.muratcangzm.model.template.TemplateTextStyleSpec
import com.muratcangzm.model.template.TemplateTimingStrategy
import com.muratcangzm.model.template.TextAlignmentPreset
import com.muratcangzm.model.template.TextAnimationPreset
import com.muratcangzm.model.template.TextCaseMode
import com.muratcangzm.model.template.TextFieldSpec
import com.muratcangzm.model.template.TextFieldType
import com.muratcangzm.model.template.TransitionPreset
import com.muratcangzm.templateengine.raw.RawTemplateDefinition
import com.muratcangzm.templateengine.raw.RawTimingStrategyDefinition

class DefaultTemplateSpecParser : TemplateSpecParser {

    override fun parse(definition: RawTemplateDefinition): TemplateSpec {
        val slots = definition.slots.map { rawSlot ->
            TemplateSlotSpec(
                id = SlotId(rawSlot.id.trim()),
                index = rawSlot.index,
                acceptedType = parseEnum<SlotAcceptedType>(rawSlot.acceptedType, "acceptedType"),
                minDurationMs = rawSlot.minDurationMs,
                maxDurationMs = rawSlot.maxDurationMs,
                preferredDurationMs = rawSlot.preferredDurationMs,
                fillMode = parseEnum<SlotFillMode>(rawSlot.fillMode, "fillMode"),
                isHero = rawSlot.isHero,
                allowUserTrim = rawSlot.allowUserTrim,
            )
        }

        val textFields = definition.textFields.map { rawText ->
            TextFieldSpec(
                id = TextFieldId(rawText.id.trim()),
                type = parseEnum<TextFieldType>(rawText.type, "textField.type"),
                label = rawText.label.trim(),
                placeholder = rawText.placeholder.trim(),
                defaultValue = rawText.defaultValue,
                maxLength = rawText.maxLength,
                required = rawText.required,
                style = TemplateTextStyleSpec(
                    maxLines = rawText.style.maxLines,
                    fontScale = rawText.style.fontScale,
                    alignment = parseEnum<TextAlignmentPreset>(
                        rawText.style.alignment,
                        "textField.style.alignment",
                    ),
                    animation = parseEnum<TextAnimationPreset>(
                        rawText.style.animation,
                        "textField.style.animation",
                    ),
                    caseMode = parseEnum<TextCaseMode>(
                        rawText.style.caseMode,
                        "textField.style.caseMode",
                    ),
                ),
            )
        }

        val timingStrategy = when (val rawTiming = definition.timingStrategy) {
            is RawTimingStrategyDefinition.FixedPerSlot -> {
                TemplateTimingStrategy.FixedPerSlot(
                    durationMs = rawTiming.durationMs,
                    allowUserDurationOverrides = rawTiming.allowUserDurationOverrides,
                )
            }

            is RawTimingStrategyDefinition.DistributedByCount -> {
                TemplateTimingStrategy.DistributedByCount(
                    totalDurationMs = rawTiming.totalDurationMs,
                    minPerSlotMs = rawTiming.minPerSlotMs,
                    maxPerSlotMs = rawTiming.maxPerSlotMs,
                    allowUserDurationOverrides = rawTiming.allowUserDurationOverrides,
                )
            }

            is RawTimingStrategyDefinition.AudioDriven -> {
                TemplateTimingStrategy.AudioDriven(
                    minPerSlotMs = rawTiming.minPerSlotMs,
                    maxPerSlotMs = rawTiming.maxPerSlotMs,
                    allowUserDurationOverrides = rawTiming.allowUserDurationOverrides,
                )
            }
        }

        val preview = TemplatePreviewSpec(
            coverSource = parseEnum<TemplateCoverSource>(
                definition.preview.coverSource,
                "preview.coverSource",
            ),
            coverSlotId = definition.preview.coverSlotId?.let(::SlotId),
            previewVideoAssetName = definition.preview.previewVideoAssetName,
        )

        val musicPolicy = TemplateMusicPolicy(
            selectionPolicy = parseEnum<AudioSelectionPolicy>(
                definition.musicPolicy.selectionPolicy,
                "musicPolicy.selectionPolicy",
            ),
            preserveOriginalClipAudio = definition.musicPolicy.preserveOriginalClipAudio,
            clipAudioVolume = definition.musicPolicy.clipAudioVolume,
            musicVolume = definition.musicPolicy.musicVolume,
            trimBehavior = parseEnum<AudioTrimBehavior>(
                definition.musicPolicy.trimBehavior,
                "musicPolicy.trimBehavior",
            ),
        )

        return TemplateSpec(
            id = TemplateId(definition.id.trim()),
            name = definition.name.trim(),
            description = definition.description.trim(),
            category = parseEnum<TemplateCategory>(definition.category, "category"),
            aspectRatio = parseEnum<AspectRatio>(definition.aspectRatio, "aspectRatio"),
            tags = definition.tags
                .map(String::trim)
                .filter(String::isNotBlank)
                .toSet(),
            minMediaCount = definition.minMediaCount,
            maxMediaCount = definition.maxMediaCount,
            slots = slots,
            textFields = textFields,
            timingStrategy = timingStrategy,
            defaultTransition = parseEnum<TransitionPreset>(
                definition.defaultTransition,
                "defaultTransition",
            ),
            musicPolicy = musicPolicy,
            preview = preview,
            isPremium = definition.isPremium,
            isFeatured = definition.isFeatured,
            sortOrder = definition.sortOrder,
        )
    }

    private inline fun <reified T : Enum<T>> parseEnum(
        rawValue: String,
        fieldName: String,
    ): T {
        val normalized = rawValue.trim()
        return enumValues<T>().firstOrNull { it.name.equals(normalized, ignoreCase = true) }
            ?: error("Unsupported value '$rawValue' for field '$fieldName'.")
    }
}