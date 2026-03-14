package com.muratcangzm.media.data.export

import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.ScaleAndRotateTransformation
import com.muratcangzm.media.domain.export.ExportVisualStyle

internal object Media3VisualStyleMapper {

    @OptIn(UnstableApi::class)
    fun map(style: ExportVisualStyle?): List<Effect> {
        if (style == null) return emptyList()

        return when (style.presetName.uppercase()) {
            "CUT" -> emptyList()

            "FADE" -> listOf(
                ScaleAndRotateTransformation.Builder()
                    .setScale(0.985f, 0.985f)
                    .build()
            )

            "ZOOM_IN" -> listOf(
                ScaleAndRotateTransformation.Builder()
                    .setScale(1.06f, 1.06f)
                    .build()
            )

            "ZOOM_OUT" -> listOf(
                ScaleAndRotateTransformation.Builder()
                    .setScale(0.94f, 0.94f)
                    .build()
            )

            "GLITCH_RGB" -> listOf(
                RgbFilter.createInvertedFilter()
            )

            "SHAKE" -> listOf(
                ScaleAndRotateTransformation.Builder()
                    .setScale(1.02f, 1.02f)
                    .build()
            )

            "SLIDE_LEFT",
            "SLIDE_RIGHT",
            "FLASH",
            "BLUR" -> emptyList()

            else -> emptyList()
        }
    }
}