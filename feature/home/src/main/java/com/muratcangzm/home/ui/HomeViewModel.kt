package com.muratcangzm.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel: ViewModel(), HomeContract.Presenter {

    private val _state = MutableStateFlow(HomeContract.State())
    override val state: StateFlow<HomeContract.State> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<HomeContract.Effect>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val effects: SharedFlow<HomeContract.Effect> = _effects.asSharedFlow()

    init {
        loadRecentProjects()
    }

    override fun onEvent(event: HomeContract.Event) {
        when (event) {
            HomeContract.Event.NewProjectClicked -> {
                _effects.tryEmit(HomeContract.Effect.NavigateToEditor(emptyList()))
            }
            is HomeContract.Event.OpenProjectClicked -> {
                _effects.tryEmit(HomeContract.Effect.NavigateToExistingProject(event.projectId))
            }
        }
    }

    fun onMediaSelected(uris: List<String>) {
        if (uris.isNotEmpty()) {
            _effects.tryEmit(HomeContract.Effect.NavigateToEditor(uris))
        }
    }

    private fun loadRecentProjects() {
        viewModelScope.launch {
            _state.value = HomeContract.State(isLoading = false, recentProjects = emptyList())
        }
    }
}
