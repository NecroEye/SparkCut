package com.muratcangzm.home.ui

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface HomeContract {

    @Immutable
    data class State(
        val isLoading: Boolean = false,
        val recentProjects: List<RecentProjectItem> = emptyList(),
    )

    @Immutable
    data class RecentProjectItem(
        val projectId: String,
        val name: String,
        val thumbnailUri: String?,
        val lastEditedLabel: String,
        val mediaCount: Int,
    )

    sealed interface Event {
        data object NewProjectClicked : Event
        data class OpenProjectClicked(val projectId: String) : Event
    }

    sealed interface Effect {
        data class NavigateToEditor(val mediaUris: List<String>) : Effect
        data class NavigateToExistingProject(val projectId: String) : Effect
    }

    interface Presenter {
        val state: StateFlow<State>
        val effects: SharedFlow<Effect>
        fun onEvent(event: Event)
    }
}
