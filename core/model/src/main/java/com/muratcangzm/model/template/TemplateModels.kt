package com.muratcangzm.model.template

import com.muratcangzm.model.id.SlotId
import com.muratcangzm.model.id.TemplateId
import com.muratcangzm.model.id.TextFieldId

enum class TemplateCategory {
    TRENDING,
    PARTY,
    LOVE,
    TRAVEL,
    FITNESS,
    PROMO,
    GLITCH,
    BIRTHDAY,
    MEMORIES,
    BUSINESS,
    MINIMAL,
    CINEMATIC,
}

enum class AspectRatio(val width: Int, val height: Int) {
    VERTICAL_9_16(9, 16),
    PORTRAIT_4_5(4, 5),
    SQUARE_1_1(1, 1),
    LANDSCAPE_4_3(4, 3),
    LANDSCAPE_16_9(16, 9);

    val ratio: Float
        get() = width.toFloat() / height.toFloat()

    companion object {
        fun fromLabel(label: String): AspectRatio? = when (label) {
            "9:16" -> VERTICAL_9_16
            "4:5" -> PORTRAIT_4_5
            "1:1" -> SQUARE_1_1
            "4:3" -> LANDSCAPE_4_3
            "16:9" -> LANDSCAPE_16_9
            else -> null
        }
    }
}

enum class SlotAcceptedType {
    IMAGE_ONLY,
    VIDEO_ONLY,
    ANY,
}

enum class SlotFillMode {
    CENTER_CROP,
    FIT_CENTER,
    BLUR_EXTEND,
}

enum class TransitionPreset {
    CUT,
    FADE,
    SLIDE_LEFT,
    SLIDE_RIGHT,
    ZOOM_IN,
    ZOOM_OUT,
    GLITCH_RGB,
    SHAKE,
    FLASH,
    BLUR,
}

enum class TextFieldType {
    TITLE,
    SUBTITLE,
    CAPTION,
    CTA,
    PRICE,
    LABEL,
}

enum class TextAlignmentPreset {
    START,
    CENTER,
    END,
}

enum class TextAnimationPreset {
    NONE,
    FADE,
    POP,
    SLIDE_UP,
    TYPEWRITER,
    GLITCH,
}

enum class TextCaseMode {
    ORIGINAL,
    UPPERCASE,
    LOWERCASE,
    TITLE_CASE,
}

enum class TemplateCoverSource {
    FIRST_MEDIA,
    LAST_MEDIA,
    CUSTOM_SLOT,
}

enum class AudioSelectionPolicy {
    OPTIONAL,
    RECOMMENDED,
    REQUIRED,
}

enum class AudioTrimBehavior {
    AUTO_FIT,
    LOOP,
    FADE_OUT,
}

data class TemplateTextStyleSpec(
    val maxLines: Int = 2,
    val fontScale: Float = 1f,
    val alignment: TextAlignmentPreset = TextAlignmentPreset.CENTER,
    val animation: TextAnimationPreset = TextAnimationPreset.NONE,
    val caseMode: TextCaseMode = TextCaseMode.ORIGINAL,
) {
    init {
        require(maxLines > 0) { "maxLines must be greater than 0." }
        require(fontScale > 0f) { "fontScale must be greater than 0." }
    }
}

data class TextFieldSpec(
    val id: TextFieldId,
    val type: TextFieldType,
    val label: String,
    val placeholder: String,
    val defaultValue: String = "",
    val maxLength: Int = 32,
    val required: Boolean = false,
    val style: TemplateTextStyleSpec = TemplateTextStyleSpec(),
) {
    init {
        require(label.isNotBlank()) { "Text field label cannot be blank." }
        require(placeholder.isNotBlank()) { "Text field placeholder cannot be blank." }
        require(maxLength > 0) { "maxLength must be greater than 0." }
        require(defaultValue.length <= maxLength) {
            "defaultValue length cannot exceed maxLength."
        }
    }
}

data class TemplateSlotSpec(
    val id: SlotId,
    val index: Int,
    val acceptedType: SlotAcceptedType = SlotAcceptedType.ANY,
    val minDurationMs: Long? = null,
    val maxDurationMs: Long? = null,
    val preferredDurationMs: Long? = null,
    val fillMode: SlotFillMode = SlotFillMode.CENTER_CROP,
    val isHero: Boolean = false,
    val allowUserTrim: Boolean = true,
) {
    init {
        require(index >= 0) { "index must be >= 0." }
        require(minDurationMs == null || minDurationMs > 0L) {
            "minDurationMs must be > 0."
        }
        require(maxDurationMs == null || maxDurationMs > 0L) {
            "maxDurationMs must be > 0."
        }
        require(preferredDurationMs == null || preferredDurationMs > 0L) {
            "preferredDurationMs must be > 0."
        }

        if (minDurationMs != null && maxDurationMs != null) {
            require(minDurationMs <= maxDurationMs) {
                "minDurationMs cannot be greater than maxDurationMs."
            }
        }

        if (preferredDurationMs != null && minDurationMs != null) {
            require(preferredDurationMs >= minDurationMs) {
                "preferredDurationMs cannot be less than minDurationMs."
            }
        }

        if (preferredDurationMs != null && maxDurationMs != null) {
            require(preferredDurationMs <= maxDurationMs) {
                "preferredDurationMs cannot be greater than maxDurationMs."
            }
        }
    }
}

sealed interface TemplateTimingStrategy {
    val allowUserDurationOverrides: Boolean

    data class FixedPerSlot(
        val durationMs: Long,
        override val allowUserDurationOverrides: Boolean = true,
    ) : TemplateTimingStrategy {
        init {
            require(durationMs > 0L) { "durationMs must be greater than 0." }
        }
    }

    data class DistributedByCount(
        val totalDurationMs: Long,
        val minPerSlotMs: Long = 800L,
        val maxPerSlotMs: Long = 3500L,
        override val allowUserDurationOverrides: Boolean = true,
    ) : TemplateTimingStrategy {
        init {
            require(totalDurationMs > 0L) { "totalDurationMs must be greater than 0." }
            require(minPerSlotMs > 0L) { "minPerSlotMs must be greater than 0." }
            require(maxPerSlotMs > 0L) { "maxPerSlotMs must be greater than 0." }
            require(minPerSlotMs <= maxPerSlotMs) {
                "minPerSlotMs cannot be greater than maxPerSlotMs."
            }
        }
    }

    data class AudioDriven(
        val minPerSlotMs: Long = 700L,
        val maxPerSlotMs: Long = 2500L,
        override val allowUserDurationOverrides: Boolean = true,
    ) : TemplateTimingStrategy {
        init {
            require(minPerSlotMs > 0L) { "minPerSlotMs must be greater than 0." }
            require(maxPerSlotMs > 0L) { "maxPerSlotMs must be greater than 0." }
            require(minPerSlotMs <= maxPerSlotMs) {
                "minPerSlotMs cannot be greater than maxPerSlotMs."
            }
        }
    }
}

data class TemplateMusicPolicy(
    val selectionPolicy: AudioSelectionPolicy = AudioSelectionPolicy.RECOMMENDED,
    val preserveOriginalClipAudio: Boolean = false,
    val clipAudioVolume: Float = 0f,
    val musicVolume: Float = 1f,
    val trimBehavior: AudioTrimBehavior = AudioTrimBehavior.AUTO_FIT,
) {
    init {
        require(clipAudioVolume in 0f..1f) {
            "clipAudioVolume must be between 0f and 1f."
        }
        require(musicVolume in 0f..1f) {
            "musicVolume must be between 0f and 1f."
        }
    }
}

data class TemplatePreviewSpec(
    val coverSource: TemplateCoverSource = TemplateCoverSource.FIRST_MEDIA,
    val coverSlotId: SlotId? = null,
    val previewVideoAssetName: String? = null,
) {
    init {
        if (coverSource == TemplateCoverSource.CUSTOM_SLOT) {
            require(coverSlotId != null) {
                "coverSlotId is required when coverSource is CUSTOM_SLOT."
            }
        }
    }
}

data class TemplateSpec(
    val id: TemplateId,
    val name: String,
    val description: String,
    val category: TemplateCategory,
    val aspectRatio: AspectRatio = AspectRatio.VERTICAL_9_16,
    val tags: Set<String> = emptySet(),
    val minMediaCount: Int,
    val maxMediaCount: Int,
    val slots: List<TemplateSlotSpec>,
    val textFields: List<TextFieldSpec> = emptyList(),
    val timingStrategy: TemplateTimingStrategy,
    val defaultTransition: TransitionPreset = TransitionPreset.FADE,
    val musicPolicy: TemplateMusicPolicy = TemplateMusicPolicy(),
    val preview: TemplatePreviewSpec = TemplatePreviewSpec(),
    val isPremium: Boolean = false,
    val isFeatured: Boolean = false,
    val sortOrder: Int = 0,
) {
    init {
        require(name.isNotBlank()) { "Template name cannot be blank." }
        require(description.isNotBlank()) { "Template description cannot be blank." }
        require(minMediaCount > 0) { "minMediaCount must be greater than 0." }
        require(maxMediaCount >= minMediaCount) {
            "maxMediaCount must be greater than or equal to minMediaCount."
        }
        require(slots.isNotEmpty()) { "Template must contain at least one slot." }
        require(maxMediaCount <= slots.size) {
            "maxMediaCount cannot exceed slot count in slot-based templates."
        }

        val slotIds = slots.map { it.id.value }
        require(slotIds.distinct().size == slotIds.size) {
            "Template slot ids must be unique."
        }

        val slotIndices = slots.map { it.index }
        require(slotIndices.distinct().size == slotIndices.size) {
            "Template slot indices must be unique."
        }

        val textFieldIds = textFields.map { it.id.value }
        require(textFieldIds.distinct().size == textFieldIds.size) {
            "Template text field ids must be unique."
        }
    }

    val mediaSlotCount: Int
        get() = slots.size

    val requiredTextFieldCount: Int
        get() = textFields.count { it.required }
}