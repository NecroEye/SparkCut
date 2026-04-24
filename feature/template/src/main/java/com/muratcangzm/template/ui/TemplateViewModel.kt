package com.muratcangzm.template.ui

import androidx.lifecycle.ViewModel
import com.muratcangzm.model.template.AspectRatio
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
import kotlinx.coroutines.flow.update

class TemplateViewModel(
    private val templateCatalog: TemplateCatalog,
) : ViewModel(), TemplateContract.Presenter {

    private val allTemplates: List<TemplateSpec> = templateCatalog.getAll()

    private val _state = MutableStateFlow(TemplateContract.State())
    override val state: StateFlow<TemplateContract.State> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<TemplateContract.Effect>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val effects: SharedFlow<TemplateContract.Effect> = _effects.asSharedFlow()

    init {
        refreshState()
    }

    override fun onEvent(event: TemplateContract.Event) {
        when (event) {
            is TemplateContract.Event.QueryChanged -> {
                _state.update { it.copy(query = event.value) }
                refreshState()
            }

            is TemplateContract.Event.CategorySelected -> {
                _state.update { it.copy(selectedCategory = event.category) }
                refreshState()
            }

            is TemplateContract.Event.TemplateClicked -> {
                _effects.tryEmit(TemplateContract.Effect.NavigateToCreate(event.templateId))
            }

            TemplateContract.Event.BackClicked -> {
                _effects.tryEmit(TemplateContract.Effect.NavigateBack)
            }
        }
    }

    private fun refreshState() {
        val selectedCategory = _state.value.selectedCategory
        val query = _state.value.query.trim()

        val filtered = allTemplates
            .asSequence()
            .filter { template ->
                selectedCategory == null || template.category == selectedCategory
            }
            .filter { template ->
                query.isBlank() || template.matchesQuery(query)
            }
            .sortedWith(
                compareByDescending<TemplateSpec> { it.isFeatured }
                    .thenBy { it.sortOrder }
                    .thenBy { it.name }
            )
            .map(::toUiItem)
            .toList()

        val categories = buildList {
            add(
                TemplateContract.CategoryItem(
                    category = null,
                    label = "All",
                    isSelected = selectedCategory == null,
                )
            )

            allTemplates
                .map { it.category }
                .distinct()
                .sortedBy { it.displayLabel() }
                .forEach { category ->
                    add(
                        TemplateContract.CategoryItem(
                            category = category,
                            label = category.displayLabel(),
                            isSelected = selectedCategory == category,
                        )
                    )
                }
        }

        _state.update {
            it.copy(
                isLoading = false,
                categories = categories,
                templates = filtered,
                resultCountLabel = "${filtered.size} templates",
            )
        }
    }

    private fun TemplateSpec.matchesQuery(query: String): Boolean {
        val normalized = query.lowercase()
        return name.lowercase().contains(normalized) ||
                description.lowercase().contains(normalized) ||
                tags.any { tag -> tag.lowercase().contains(normalized) } ||
                category.name.lowercase().contains(normalized)
    }

    private fun toUiItem(template: TemplateSpec): TemplateContract.TemplateItem =
        TemplateContract.TemplateItem(
            id = template.id,
            name = template.name,
            description = template.description,
            categoryLabel = template.category.displayLabel(),
            aspectRatioLabel = template.aspectRatio.displayLabel(),
            tags = template.tags.take(3).toList(),
            minMediaCount = template.minMediaCount,
            maxMediaCount = template.maxMediaCount,
            isFeatured = template.isFeatured,
            isPremium = template.isPremium,
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

private fun AspectRatio.displayLabel(): String = when (this) {
    AspectRatio.VERTICAL_9_16 -> "9:16"
    AspectRatio.PORTRAIT_4_5 -> "4:5"
    AspectRatio.SQUARE_1_1 -> "1:1"
    AspectRatio.LANDSCAPE_4_3 -> "4:3"
    AspectRatio.LANDSCAPE_16_9 -> "16:9"
}