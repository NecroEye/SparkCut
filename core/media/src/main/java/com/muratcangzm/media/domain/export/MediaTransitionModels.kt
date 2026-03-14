package com.muratcangzm.media.domain.export

import com.muratcangzm.model.template.TransitionPreset

enum class MediaTransitionPhase {
    INTRO,
    OUTRO,
}

data class MediaTransitionWindow(
    val preset: TransitionPreset,
    val targetIndex: Int,
    val phase: MediaTransitionPhase,
    val durationMs: Long,
)