package com.muratcangzm.templateengine.planner

import com.muratcangzm.model.template.TransitionPreset

data class PlannedTransitionWindow(
    val preset: TransitionPreset,
    val targetIndex: Int,
    val phase: TransitionPhase,
    val durationMs: Long,
)

enum class TransitionPhase {
    OUTRO,
    INTRO,
}