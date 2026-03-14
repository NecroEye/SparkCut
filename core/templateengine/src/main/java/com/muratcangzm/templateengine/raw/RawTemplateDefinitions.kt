package com.muratcangzm.templateengine.raw

data class RawTemplateDefinition(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val aspectRatio: String = "VERTICAL_9_16",
    val tags: List<String> = emptyList(),
    val minMediaCount: Int,
    val maxMediaCount: Int,
    val slots: List<RawTemplateSlotDefinition>,
    val textFields: List<RawTextFieldDefinition> = emptyList(),
    val timingStrategy: RawTimingStrategyDefinition,
    val defaultTransition: String = "FADE",
    val musicPolicy: RawTemplateMusicPolicyDefinition = RawTemplateMusicPolicyDefinition(),
    val preview: RawTemplatePreviewDefinition = RawTemplatePreviewDefinition(),
    val isPremium: Boolean = false,
    val isFeatured: Boolean = false,
    val sortOrder: Int = 0,
)

data class RawTemplateSlotDefinition(
    val id: String,
    val index: Int,
    val acceptedType: String = "ANY",
    val minDurationMs: Long? = null,
    val maxDurationMs: Long? = null,
    val preferredDurationMs: Long? = null,
    val fillMode: String = "CENTER_CROP",
    val isHero: Boolean = false,
    val allowUserTrim: Boolean = true,
)

data class RawTemplateTextStyleDefinition(
    val maxLines: Int = 2,
    val fontScale: Float = 1f,
    val alignment: String = "CENTER",
    val animation: String = "NONE",
    val caseMode: String = "ORIGINAL",
)

data class RawTextFieldDefinition(
    val id: String,
    val type: String,
    val label: String,
    val placeholder: String,
    val defaultValue: String = "",
    val maxLength: Int = 32,
    val required: Boolean = false,
    val style: RawTemplateTextStyleDefinition = RawTemplateTextStyleDefinition(),
)

sealed interface RawTimingStrategyDefinition {
    data class FixedPerSlot(
        val durationMs: Long,
        val allowUserDurationOverrides: Boolean = true,
    ) : RawTimingStrategyDefinition

    data class DistributedByCount(
        val totalDurationMs: Long,
        val minPerSlotMs: Long = 800L,
        val maxPerSlotMs: Long = 3500L,
        val allowUserDurationOverrides: Boolean = true,
    ) : RawTimingStrategyDefinition

    data class AudioDriven(
        val minPerSlotMs: Long = 700L,
        val maxPerSlotMs: Long = 2500L,
        val allowUserDurationOverrides: Boolean = true,
    ) : RawTimingStrategyDefinition
}

data class RawTemplateMusicPolicyDefinition(
    val selectionPolicy: String = "RECOMMENDED",
    val preserveOriginalClipAudio: Boolean = false,
    val clipAudioVolume: Float = 0f,
    val musicVolume: Float = 1f,
    val trimBehavior: String = "AUTO_FIT",
)

data class RawTemplatePreviewDefinition(
    val coverSource: String = "FIRST_MEDIA",
    val coverSlotId: String? = null,
    val previewVideoAssetName: String? = null,
)