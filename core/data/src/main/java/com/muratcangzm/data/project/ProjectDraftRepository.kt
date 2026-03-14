package com.muratcangzm.data.project

import com.muratcangzm.model.id.ProjectId
import com.muratcangzm.model.project.ProjectEditorSession
import com.muratcangzm.model.project.ProjectListItem
import com.muratcangzm.model.project.ProjectStatus
import kotlinx.coroutines.flow.Flow

interface ProjectDraftRepository {
    fun observeProjectListItems(): Flow<List<ProjectListItem>>
    fun observeProjectSession(projectId: ProjectId): Flow<ProjectEditorSession?>
    suspend fun getProjectSession(projectId: ProjectId): ProjectEditorSession?
    suspend fun saveSession(session: ProjectEditorSession)
    suspend fun updateStatus(
        projectId: ProjectId,
        status: ProjectStatus,
        updatedAtEpochMillis: Long,
    )
    suspend fun deleteProject(projectId: ProjectId)
}
