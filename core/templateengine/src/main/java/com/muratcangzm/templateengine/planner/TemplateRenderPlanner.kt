package com.muratcangzm.templateengine.planner

import com.muratcangzm.model.media.MediaAsset
import com.muratcangzm.model.template.TemplateSpec

interface TemplateRenderPlanner {
    fun createPlan(
        template: TemplateSpec,
        assets: List<MediaAsset>,
        textValues: Map<String, String>,
    ): TemplateRenderPlan
}