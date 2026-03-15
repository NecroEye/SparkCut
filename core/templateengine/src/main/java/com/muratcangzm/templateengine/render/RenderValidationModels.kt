package com.muratcangzm.templateengine.render

enum class RenderValidationSeverity {
    WARNING,
    ERROR,
}

enum class RenderValidationCode {
    TEMPLATE_NOT_FOUND,
    SLOT_COUNT_BELOW_MIN,
    SLOT_COUNT_ABOVE_MAX,
    SLOT_BINDING_REFERENCES_UNKNOWN_ASSET,
    SLOT_BINDING_DUPLICATE_ORDER,
    SLOT_BINDING_DUPLICATE_SLOT,
    REQUIRED_TEXT_MISSING,
    TEXT_VALUE_TOO_LONG,
    VIDEO_TRIM_INVALID,
    VIDEO_TRIM_OUT_OF_RANGE,
    COVER_ASSET_MISSING,
    AUDIO_SELECTION_INVALID,
}

data class RenderValidationIssue(
    val code: RenderValidationCode,
    val severity: RenderValidationSeverity,
    val message: String,
    val field: String? = null,
)

data class RenderValidationResult(
    val issues: List<RenderValidationIssue>,
) {
    val hasErrors: Boolean
        get() = issues.any { it.severity == RenderValidationSeverity.ERROR }

    val hasWarnings: Boolean
        get() = issues.any { it.severity == RenderValidationSeverity.WARNING }

    val isValid: Boolean
        get() = !hasErrors
}
