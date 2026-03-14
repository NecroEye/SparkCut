package com.muratcangzm.media.domain.export

enum class ExportTextGravity {
    TOP_CENTER,
    CENTER,
    BOTTOM_CENTER,
}

data class ExportTextOverlay(
    val id: String,
    val text: String,
    val gravity: ExportTextGravity,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long? = null,
    val textSizeSp: Float = 20f,
    val paddingDp: Float = 16f,
)

data class ExportVisualStyle(
    val presetName: String,
)