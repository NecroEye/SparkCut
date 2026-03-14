package com.muratcangzm.data.projectsession

import com.muratcangzm.data.project.ProjectDraftRepository
import com.muratcangzm.data.session.ProjectSessionStore
import com.muratcangzm.model.id.ProjectId
import com.muratcangzm.model.project.ProjectEditorSession
import com.muratcangzm.model.project.ProjectListItem
import com.muratcangzm.model.project.ProjectStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class DefaultProjectSessionManager(
    private val repository: ProjectDraftRepository,
    private val sessionStore: ProjectSessionStore,
) : ProjectSessionManager {

    override val lastActiveProjectId: Flow<ProjectId?> =
        sessionStore.lastActiveProjectId

    override fun observeProjects(): Flow<List<ProjectListItem>> =
        repository.observeProjectListItems()

    override fun observeSession(projectId: ProjectId): Flow<ProjectEditorSession?> =
        repository.observeProjectSession(projectId)

    override suspend fun getSession(projectId: ProjectId): ProjectEditorSession? =
        repository.getProjectSession(projectId)

    override suspend fun saveSession(
        session: ProjectEditorSession,
        setActive: Boolean,
    ) {
        repository.saveSession(session)
        if (setActive) {
            sessionStore.setLastActiveProjectId(session.draft.id)
        }
    }

    override suspend fun setLastActive(projectId: ProjectId?) {
        sessionStore.setLastActiveProjectId(projectId)
    }

    override suspend fun updateStatus(
        projectId: ProjectId,
        status: ProjectStatus,
        updatedAtEpochMillis: Long,
    ) {
        repository.updateStatus(
            projectId = projectId,
            status = status,
            updatedAtEpochMillis = updatedAtEpochMillis,
        )
        sessionStore.setLastActiveProjectId(projectId)
    }

    override suspend fun deleteProject(projectId: ProjectId) {
        val currentLastActive = sessionStore.lastActiveProjectId.first()
        repository.deleteProject(projectId)
        if (currentLastActive?.value == projectId.value) {
            sessionStore.setLastActiveProjectId(null)
        }
    }
}
