package com.muratcangzm.model.project

import com.muratcangzm.model.id.ProjectId
import com.muratcangzm.model.id.TemplateId
import com.muratcangzm.model.template.AspectRatio

data class ProjectListItem(
    val id: ProjectId,
    val name: String,
    val templateId: TemplateId,
    val aspectRatio: AspectRatio,
    val mediaCount: Int,
    val updatedAtEpochMillis: Long,
    val status: ProjectStatus,
)
