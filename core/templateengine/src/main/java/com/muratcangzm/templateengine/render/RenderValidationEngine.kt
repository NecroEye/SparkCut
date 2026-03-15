package com.muratcangzm.templateengine.render

import com.muratcangzm.model.project.ProjectEditorSession
import com.muratcangzm.model.template.TemplateSpec

interface RenderValidationEngine {
    fun validate(
        template: TemplateSpec,
        session: ProjectEditorSession,
    ): RenderValidationResult
}
