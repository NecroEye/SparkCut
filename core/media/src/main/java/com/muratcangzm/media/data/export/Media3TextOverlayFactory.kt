package com.muratcangzm.media.data.export

import com.muratcangzm.media.domain.export.ExportTextGravity
import com.muratcangzm.media.domain.export.ExportTextOverlay

internal object Media3TextOverlayFactory {

    fun hasOverlays(overlays: List<ExportTextOverlay>): Boolean =
        overlays.any { it.text.isNotBlank() }

    fun normalizeGravity(gravity: ExportTextGravity): Pair<Float, Float> = when (gravity) {
        ExportTextGravity.TOP_CENTER -> 0f to 0.78f
        ExportTextGravity.CENTER -> 0f to 0f
        ExportTextGravity.BOTTOM_CENTER -> 0f to -0.78f
    }
}