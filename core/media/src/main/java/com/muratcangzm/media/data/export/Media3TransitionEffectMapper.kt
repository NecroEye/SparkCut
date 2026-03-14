package com.muratcangzm.media.data.export

import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.ScaleAndRotateTransformation
import com.muratcangzm.media.domain.export.MediaTransitionPhase
import com.muratcangzm.media.domain.export.MediaTransitionWindow
import com.muratcangzm.model.template.TransitionPreset

internal object Media3TransitionEffectMapper {

    @OptIn(UnstableApi::class)
    fun map(
        transition: MediaTransitionWindow?,
    ): List<Effect> {
        if (transition == null) return emptyList()

        return when (transition.preset) {
            TransitionPreset.CUT -> emptyList()

            TransitionPreset.FADE -> when (transition.phase) {
                MediaTransitionPhase.INTRO -> listOf(
                    ScaleAndRotateTransformation.Builder()
                        .setScale(1.04f, 1.04f)
                        .build()
                )

                MediaTransitionPhase.OUTRO -> listOf(
                    ScaleAndRotateTransformation.Builder()
                        .setScale(0.96f, 0.96f)
                        .build()
                )
            }

            TransitionPreset.ZOOM_IN -> listOf(
                ScaleAndRotateTransformation.Builder()
                    .setScale(1.08f, 1.08f)
                    .build()
            )

            TransitionPreset.ZOOM_OUT -> listOf(
                ScaleAndRotateTransformation.Builder()
                    .setScale(0.92f, 0.92f)
                    .build()
            )

            TransitionPreset.SHAKE -> listOf(
                ScaleAndRotateTransformation.Builder()
                    .setScale(1.03f, 1.03f)
                    .build()
            )

            TransitionPreset.GLITCH_RGB -> listOf(
                RgbFilter.createInvertedFilter()
            )

            TransitionPreset.SLIDE_LEFT,
            TransitionPreset.SLIDE_RIGHT,
            TransitionPreset.FLASH,
            TransitionPreset.BLUR -> emptyList()
        }
    }
}