package com.muratcangzm.template.ui

import androidx.compose.runtime.Immutable
import com.muratcangzm.model.id.TemplateId
import com.muratcangzm.model.template.TemplateCategory
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface TemplateContract {

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val query: String = "",
        val selectedCategory: TemplateCategory? = null,
        val categories: List<CategoryItem> = emptyList(),
        val templates: List<TemplateItem> = emptyList(),
        val resultCountLabel: String = "",
    ) {
        val isEmpty: Boolean
            get() = !isLoading && templates.isEmpty()
    }

    @Immutable
    data class CategoryItem(
        val category: TemplateCategory?,
        val label: String,
        val isSelected: Boolean,
    )

    @Immutable
    data class TemplateItem(
        val id: TemplateId,
        val name: String,
        val description: String,
        val categoryLabel: String,
        val aspectRatioLabel: String,
        val tags: List<String>,
        val minMediaCount: Int,
        val maxMediaCount: Int,
        val isFeatured: Boolean,
        val isPremium: Boolean,
    )

    sealed interface Event {
        data class QueryChanged(val value: String) : Event
        data class CategorySelected(val category: TemplateCategory?) : Event
        data class TemplateClicked(val templateId: TemplateId) : Event
        data object BackClicked : Event
    }

    sealed interface Effect {
        data class NavigateToCreate(val templateId: TemplateId) : Effect
        data object NavigateBack : Effect
    }

    interface Presenter {
        val state: StateFlow<State>
        val effects: SharedFlow<Effect>
        fun onEvent(event: Event)
    }
}