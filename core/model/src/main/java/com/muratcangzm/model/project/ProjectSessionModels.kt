package com.muratcangzm.model.project

import com.muratcangzm.model.id.MediaAssetId
import com.muratcangzm.model.id.ProjectId
import com.muratcangzm.model.id.TemplateId
import com.muratcangzm.model.template.AspectRatio

data class ProjectMediaAssetRef(
    val id: MediaAssetId,
    val sourceUri: String,
    val fileName: String?,
    val mimeType: String?,
    val width: Int?,
    val height: Int?,
    val durationMs: Long?,
)

data class ProjectEditorSession(
    val draft: ProjectDraft,
    val mediaAssets: List<ProjectMediaAssetRef>,
) {
    init {
        val assetIds = mediaAssets.map { it.id.value }.toSet()
        val boundIds = draft.slotBindings.map { it.mediaAssetId.value }.toSet()
        require(boundIds.all { it in assetIds }) {
            "All slot-bound mediaAssetIds must exist in mediaAssets."
        }
    }
}