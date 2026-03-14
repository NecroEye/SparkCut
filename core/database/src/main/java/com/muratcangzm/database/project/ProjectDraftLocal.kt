package com.muratcangzm.database.project

import androidx.room.Embedded
import androidx.room.Relation

data class ProjectDraftLocal(
    @Embedded
    val project: ProjectEntity,
    @Relation(
        parentColumn = "project_id",
        entityColumn = "project_owner_id",
    )
    val slotBindings: List<ProjectSlotBindingEntity>,
    @Relation(
        parentColumn = "project_id",
        entityColumn = "project_owner_id",
    )
    val textValues: List<ProjectTextValueEntity>,
    @Relation(
        parentColumn = "project_id",
        entityColumn = "project_owner_id",
    )
    val transitionOverrides: List<ProjectTransitionOverrideEntity>,
    @Relation(
        parentColumn = "project_id",
        entityColumn = "project_owner_id",
    )
    val mediaAssets: List<ProjectMediaAssetEntity>,
)
