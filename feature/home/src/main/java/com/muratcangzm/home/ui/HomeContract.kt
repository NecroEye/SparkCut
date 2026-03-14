package com.muratcangzm.home.ui

import androidx.compose.runtime.Immutable
import com.muratcangzm.model.id.TemplateId
import com.muratcangzm.model.template.TemplateCategory
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface HomeContract {

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val featuredTemplates: List<FeaturedTemplateItem> = emptyList(),
        val categories: List<CategoryItem> = emptyList(),
    )

    @Immutable
    data class FeaturedTemplateItem(
        val id: TemplateId,
        val name: String,
        val subtitle: String,
        val categoryLabel: String,
    )

    @Immutable
    data class CategoryItem(
        val category: TemplateCategory,
        val label: String,
    )

    sealed interface Event {
        data object BrowseAllTemplatesClicked : Event
        data class FeaturedTemplateClicked(val templateId: TemplateId) : Event
        data class CategoryClicked(val category: TemplateCategory) : Event
    }

    sealed interface Effect {
        data object NavigateToTemplateBrowser : Effect
        data class NavigateToCreate(val templateId: TemplateId) : Effect
        data class NavigateToCategory(val category: TemplateCategory) : Effect
    }

    interface Presenter {
        val state: StateFlow<State>
        val effects: SharedFlow<Effect>
        fun onEvent(event: Event)
    }
}