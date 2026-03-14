package com.muratcangzm.home.ui

import androidx.lifecycle.ViewModel
import com.muratcangzm.model.template.TemplateCategory
import com.muratcangzm.model.template.TemplateSpec
import com.muratcangzm.templateengine.catalog.TemplateCatalog
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel(
    private val templateCatalog: TemplateCatalog,
) : ViewModel(), HomeContract.Presenter {

    private val _state = MutableStateFlow(HomeContract.State())
    override val state: StateFlow<HomeContract.State> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<HomeContract.Effect>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val effects: SharedFlow<HomeContract.Effect> = _effects.asSharedFlow()

    init {
        val featuredTemplates = templateCatalog.getFeatured()
            .sortedBy { it.sortOrder }
            .take(6)
            .map(::toFeaturedItem)

        val categories = templateCatalog.getAll()
            .map { it.category }
            .distinct()
            .sortedBy { it.displayLabel() }
            .map { category ->
                HomeContract.CategoryItem(
                    category = category,
                    label = category.displayLabel(),
                )
            }

        _state.value = HomeContract.State(
            isLoading = false,
            featuredTemplates = featuredTemplates,
            categories = categories,
        )
    }

    override fun onEvent(event: HomeContract.Event) {
        when (event) {
            HomeContract.Event.BrowseAllTemplatesClicked -> {
                _effects.tryEmit(HomeContract.Effect.NavigateToTemplateBrowser)
            }

            is HomeContract.Event.FeaturedTemplateClicked -> {
                _effects.tryEmit(HomeContract.Effect.NavigateToCreate(event.templateId))
            }

            is HomeContract.Event.CategoryClicked -> {
                _effects.tryEmit(HomeContract.Effect.NavigateToCategory(event.category))
            }
        }
    }

    private fun toFeaturedItem(template: TemplateSpec): HomeContract.FeaturedTemplateItem =
        HomeContract.FeaturedTemplateItem(
            id = template.id,
            name = template.name,
            subtitle = template.description,
            categoryLabel = template.category.displayLabel(),
        )
}

private fun TemplateCategory.displayLabel(): String = when (this) {
    TemplateCategory.TRENDING -> "Trending"
    TemplateCategory.PARTY -> "Party"
    TemplateCategory.LOVE -> "Love"
    TemplateCategory.TRAVEL -> "Travel"
    TemplateCategory.FITNESS -> "Fitness"
    TemplateCategory.PROMO -> "Promo"
    TemplateCategory.GLITCH -> "Glitch"
    TemplateCategory.BIRTHDAY -> "Birthday"
    TemplateCategory.MEMORIES -> "Memories"
    TemplateCategory.BUSINESS -> "Business"
    TemplateCategory.MINIMAL -> "Minimal"
    TemplateCategory.CINEMATIC -> "Cinematic"
}