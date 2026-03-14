package com.muratcangzm.templateengine.planner

import com.muratcangzm.model.id.SlotId
import com.muratcangzm.model.media.MediaType

enum class PlannedOverlayGravity {
    TOP_CENTER,
    CENTER,
    BOTTOM_CENTER,
}

data class PlannedSequenceItem(
    val slotId: SlotId,
    val uri: String,
    val mediaType: MediaType,
    val durationMs: Long,
    val trimStartMs: Long? = null,
    val trimEndMs: Long? = null,
)

data class PlannedTextOverlay(
    val id: String,
    val text: String,
    val gravity: PlannedOverlayGravity,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val textSizeSp: Float,
)

data class TemplateRenderPlan(
    val totalDurationMs: Long,
    val sequenceItems: List<PlannedSequenceItem>,
    val textOverlays: List<PlannedTextOverlay>,
    val transitions: List<PlannedTransitionWindow>,
)