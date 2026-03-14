package com.muratcangzm.data.session

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.muratcangzm.model.id.ProjectId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.projectSessionDataStore by preferencesDataStore(
    name = "project_session",
)

class DataStoreProjectSessionStore(
    private val context: Context,
) : ProjectSessionStore {

    override val lastActiveProjectId: Flow<ProjectId?> =
        context.projectSessionDataStore.data.map { preferences ->
            preferences[LAST_ACTIVE_PROJECT_ID]?.let(::ProjectId)
        }

    override suspend fun setLastActiveProjectId(projectId: ProjectId?) {
        context.projectSessionDataStore.edit { preferences ->
            if (projectId == null) {
                preferences.remove(LAST_ACTIVE_PROJECT_ID)
            } else {
                preferences[LAST_ACTIVE_PROJECT_ID] = projectId.value
            }
        }
    }

    private companion object {
        val LAST_ACTIVE_PROJECT_ID: Preferences.Key<String> =
            stringPreferencesKey("last_active_project_id")
    }
}
