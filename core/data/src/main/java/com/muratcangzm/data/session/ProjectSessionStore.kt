package com.muratcangzm.data.session

import com.muratcangzm.model.id.ProjectId
import kotlinx.coroutines.flow.Flow

interface ProjectSessionStore {
    val lastActiveProjectId: Flow<ProjectId?>
    suspend fun setLastActiveProjectId(projectId: ProjectId?)
}
