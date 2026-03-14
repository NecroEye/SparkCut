package com.muratcangzm.database.project

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Transaction
    @Query(
        """
        SELECT * FROM projects
        WHERE project_id = :projectId
        LIMIT 1
        """
    )
    suspend fun getProjectDraft(projectId: String): ProjectDraftLocal?

    @Transaction
    @Query(
        """
        SELECT * FROM projects
        WHERE project_id = :projectId
        LIMIT 1
        """
    )
    fun observeProjectDraft(projectId: String): Flow<ProjectDraftLocal?>

    @Query(
        """
        SELECT
            p.project_id AS projectId,
            p.name AS name,
            p.template_id AS templateId,
            p.aspect_ratio AS aspectRatio,
            COUNT(s.row_id) AS mediaCount,
            p.updated_at_epoch_millis AS updatedAtEpochMillis,
            p.status AS status
        FROM projects p
        LEFT JOIN project_slot_bindings s
            ON s.project_owner_id = p.project_id
        GROUP BY p.project_id
        ORDER BY p.updated_at_epoch_millis DESC
        """
    )
    fun observeProjectListItems(): Flow<List<ProjectListItemLocal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlotBindings(items: List<ProjectSlotBindingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTextValues(items: List<ProjectTextValueEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransitionOverrides(items: List<ProjectTransitionOverrideEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaAssets(items: List<ProjectMediaAssetEntity>)

    @Query(
        """
        DELETE FROM project_slot_bindings
        WHERE project_owner_id = :projectId
        """
    )
    suspend fun deleteSlotBindings(projectId: String)

    @Query(
        """
        DELETE FROM project_text_values
        WHERE project_owner_id = :projectId
        """
    )
    suspend fun deleteTextValues(projectId: String)

    @Query(
        """
        DELETE FROM project_transition_overrides
        WHERE project_owner_id = :projectId
        """
    )
    suspend fun deleteTransitionOverrides(projectId: String)

    @Query(
        """
        DELETE FROM project_media_assets
        WHERE project_owner_id = :projectId
        """
    )
    suspend fun deleteMediaAssets(projectId: String)

    @Query(
        """
        DELETE FROM projects
        WHERE project_id = :projectId
        """
    )
    suspend fun deleteProject(projectId: String)

    @Query(
        """
        UPDATE projects
        SET
            status = :status,
            updated_at_epoch_millis = :updatedAtEpochMillis
        WHERE project_id = :projectId
        """
    )
    suspend fun updateProjectStatus(
        projectId: String,
        status: String,
        updatedAtEpochMillis: Long,
    )

    @Transaction
    suspend fun replaceProjectGraph(
        project: ProjectEntity,
        slotBindings: List<ProjectSlotBindingEntity>,
        textValues: List<ProjectTextValueEntity>,
        transitionOverrides: List<ProjectTransitionOverrideEntity>,
        mediaAssets: List<ProjectMediaAssetEntity>,
    ) {
        upsertProject(project)
        deleteSlotBindings(project.projectId)
        deleteTextValues(project.projectId)
        deleteTransitionOverrides(project.projectId)
        deleteMediaAssets(project.projectId)

        if (slotBindings.isNotEmpty()) insertSlotBindings(slotBindings)
        if (textValues.isNotEmpty()) insertTextValues(textValues)
        if (transitionOverrides.isNotEmpty()) insertTransitionOverrides(transitionOverrides)
        if (mediaAssets.isNotEmpty()) insertMediaAssets(mediaAssets)
    }
}
