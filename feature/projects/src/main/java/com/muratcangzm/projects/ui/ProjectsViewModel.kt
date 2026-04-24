package com.muratcangzm.projects.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muratcangzm.data.projectsession.ProjectSessionManager
import com.muratcangzm.model.id.ProjectId
import com.muratcangzm.model.project.ProjectListItem
import com.muratcangzm.model.project.ProjectStatus
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProjectsViewModel(
    private val projectSessionManager: ProjectSessionManager,
) : ViewModel(), ProjectsContract.Presenter {

    private val _state = MutableStateFlow(ProjectsContract.State())
    override val state: StateFlow<ProjectsContract.State> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ProjectsContract.Effect>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val effects: SharedFlow<ProjectsContract.Effect> = _effects.asSharedFlow()

    init {
        observeProjects()
    }

    override fun onEvent(event: ProjectsContract.Event) {
        when (event) {
            is ProjectsContract.Event.OpenProject -> {
                viewModelScope.launch {
                    projectSessionManager.setLastActive(ProjectId(event.projectId))
                    _effects.tryEmit(
                        ProjectsContract.Effect.NavigateEditor(event.projectId)
                    )
                }
            }

            is ProjectsContract.Event.DeleteProject -> {
                viewModelScope.launch {
                    projectSessionManager.deleteProject(ProjectId(event.projectId))
                    _effects.tryEmit(
                        ProjectsContract.Effect.ShowMessage("Project deleted.")
                    )
                }
            }
        }
    }

    private fun observeProjects() {
        viewModelScope.launch {
            combine(
                projectSessionManager.observeProjects(),
                projectSessionManager.lastActiveProjectId,
            ) { items, lastActive ->
                items.map { it.toUi(lastActive?.value) } to lastActive?.value
            }.collect { (items, lastActiveId) ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        items = items,
                        lastActiveProjectId = lastActiveId,
                        errorMessage = null,
                    )
                }
            }
        }
    }
}

private fun ProjectListItem.toUi(lastActiveProjectId: String?): ProjectsContract.ProjectItem =
    ProjectsContract.ProjectItem(
        id = id.value,
        name = name,
        templateId = templateId.value,
        aspectRatioLabel = aspectRatio.displayLabel(),
        mediaCount = mediaCount,
        updatedAtLabel = updatedAtEpochMillis.toUiDate(),
        statusLabel = status.displayLabel(),
        isLastActive = id.value == lastActiveProjectId,
    )

private fun Long.toUiDate(): String {
    val format = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
    return format.format(Date(this))
}

private fun ProjectStatus.displayLabel(): String = when (this) {
    ProjectStatus.DRAFT -> "Draft"
    ProjectStatus.READY -> "Ready"
    ProjectStatus.EXPORTING -> "Exporting"
    ProjectStatus.EXPORTED -> "Exported"
    ProjectStatus.FAILED -> "Failed"
}

private fun com.muratcangzm.model.template.AspectRatio.displayLabel(): String = when (this) {
    com.muratcangzm.model.template.AspectRatio.VERTICAL_9_16 -> "9:16"
    com.muratcangzm.model.template.AspectRatio.PORTRAIT_4_5 -> "4:5"
    com.muratcangzm.model.template.AspectRatio.SQUARE_1_1 -> "1:1"
    com.muratcangzm.model.template.AspectRatio.LANDSCAPE_4_3 -> "4:3"
    com.muratcangzm.model.template.AspectRatio.LANDSCAPE_16_9 -> "16:9"
}
