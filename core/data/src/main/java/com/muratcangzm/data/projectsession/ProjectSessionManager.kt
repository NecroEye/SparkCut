package com.muratcangzm.data.projectsession

import com.muratcangzm.model.id.ProjectId
import com.muratcangzm.model.project.ProjectEditorSession
import com.muratcangzm.model.project.ProjectListItem
import com.muratcangzm.model.project.ProjectStatus
import kotlinx.coroutines.flow.Flow

interface ProjectSessionManager {
    val lastActiveProjectId: Flow<ProjectId?>

    fun observeProjects(): Flow<List<ProjectListItem>>
    fun observeSession(projectId: ProjectId): Flow<ProjectEditorSession?>

    suspend fun getSession(projectId: ProjectId): ProjectEditorSession?
    suspend fun saveSession(session: ProjectEditorSession, setActive: Boolean = true)
    suspend fun setLastActive(projectId: ProjectId?)
    suspend fun updateStatus(
        projectId: ProjectId,
        status: ProjectStatus,
        updatedAtEpochMillis: Long,
    )
    suspend fun deleteProject(projectId: ProjectId)
}
