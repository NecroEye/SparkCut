package com.muratcangzm.projects.ui

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface ProjectsContract {

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val items: List<ProjectItem> = emptyList(),
        val lastActiveProjectId: String? = null,
        val errorMessage: String? = null,
    )

    @Immutable
    data class ProjectItem(
        val id: String,
        val name: String,
        val templateId: String,
        val aspectRatioLabel: String,
        val mediaCount: Int,
        val updatedAtLabel: String,
        val statusLabel: String,
        val isLastActive: Boolean,
    )

    sealed interface Event {
        data class OpenProject(val projectId: String) : Event
        data class DeleteProject(val projectId: String) : Event
    }

    sealed interface Effect {
        data class NavigateEditor(val projectId: String) : Effect
        data class ShowMessage(val message: String) : Effect
    }

    interface Presenter {
        val state: StateFlow<State>
        val effects: SharedFlow<Effect>
        fun onEvent(event: Event)
    }
}
