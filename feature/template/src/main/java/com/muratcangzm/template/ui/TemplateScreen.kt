package com.muratcangzm.template.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muratcangzm.designsystem.banner.SparkCutValidationBanner
import com.muratcangzm.designsystem.banner.SparkCutValidationBannerSeverity
import com.muratcangzm.designsystem.button.SparkCutButtonSize
import com.muratcangzm.designsystem.button.SparkCutPrimaryButton
import com.muratcangzm.designsystem.card.SparkCutCard
import com.muratcangzm.designsystem.card.SparkCutCardTone
import com.muratcangzm.designsystem.chip.SparkCutStatusChip
import com.muratcangzm.designsystem.chip.SparkCutStatusTone
import com.muratcangzm.designsystem.component.SparkCutScaffold
import com.muratcangzm.designsystem.component.SparkCutTopBar
import com.muratcangzm.designsystem.field.SparkCutTextField
import com.muratcangzm.designsystem.theme.SparkCutTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun TemplateScreen(
    onBack: () -> Unit,
    onOpenCreate: (templateId: String) -> Unit,
    viewModel: TemplateViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TemplateContract.Effect.NavigateToCreate -> {
                    onOpenCreate(effect.templateId.value)
                }

                TemplateContract.Effect.NavigateBack -> onBack()
            }
        }
    }

    TemplateScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun TemplateScreenContent(
    state: TemplateContract.State,
    onEvent: (TemplateContract.Event) -> Unit,
) {
    val spacing = SparkCutTheme.spacing
    val colors = SparkCutTheme.colors

    SparkCutScaffold(
        topBar = {
            SparkCutTopBar(
                title = "Templates",
                subtitle = if (state.isLoading) {
                    "Loading creative presets"
                } else {
                    state.resultCountLabel
                },
                navigationIcon = {
                    IconButton(
                        onClick = { onEvent(TemplateContract.Event.BackClicked) }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = colors.primary
                    )
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 220.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        TemplateHeroCard(
                            resultCountLabel = state.resultCountLabel,
                            selectedCategoryLabel = state.categories
                                .firstOrNull { it.isSelected }
                                ?.label
                                ?: "All",
                            hasQuery = state.query.isNotBlank(),
                        )
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SparkCutCard(
                            modifier = Modifier.fillMaxWidth(),
                            tone = SparkCutCardTone.Elevated
                        ) {
                            SparkCutTextField(
                                value = state.query,
                                onValueChange = {
                                    onEvent(TemplateContract.Event.QueryChanged(it))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = "Search templates",
                                placeholder = "Search by name, tag or category",
                                singleLine = true,
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                            contentPadding = PaddingValues(vertical = spacing.xxs),
                        ) {
                            items(state.categories) { category ->
                                FilterChip(
                                    selected = category.isSelected,
                                    onClick = {
                                        onEvent(
                                            TemplateContract.Event.CategorySelected(category.category)
                                        )
                                    },
                                    label = {
                                        Text(category.label)
                                    }
                                )
                            }
                        }
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                        ) {
                            SparkCutStatusChip(
                                text = state.resultCountLabel,
                                tone = SparkCutStatusTone.Info
                            )

                            if (state.query.isNotBlank()) {
                                SparkCutStatusChip(
                                    text = "Filtered",
                                    tone = SparkCutStatusTone.Accent
                                )
                            }
                        }
                    }

                    if (state.isEmpty) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SparkCutValidationBanner(
                                title = "No templates found",
                                message = "Try another keyword or switch the selected category to see more results.",
                                severity = SparkCutValidationBannerSeverity.Warning
                            )
                        }
                    } else {
                        items(
                            items = state.templates,
                            key = { it.id.value }
                        ) { item ->
                            TemplateGridCard(
                                item = item,
                                onClick = {
                                    onEvent(TemplateContract.Event.TemplateClicked(item.id))
                                }
                            )
                        }
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Spacer(
                            modifier = Modifier.navigationBarsPadding()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateHeroCard(
    resultCountLabel: String,
    selectedCategoryLabel: String,
    hasQuery: Boolean,
) {
    val spacing = SparkCutTheme.spacing
    val typography = SparkCutTheme.typography
    val colors = SparkCutTheme.colors

    SparkCutCard(
        modifier = Modifier.fillMaxWidth(),
        tone = SparkCutCardTone.Accent
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                SparkCutStatusChip(
                    text = resultCountLabel,
                    tone = SparkCutStatusTone.Info
                )
                SparkCutStatusChip(
                    text = selectedCategoryLabel,
                    tone = SparkCutStatusTone.Neutral
                )
                if (hasQuery) {
                    SparkCutStatusChip(
                        text = "Search active",
                        tone = SparkCutStatusTone.Accent
                    )
                }
            }

            Text(
                text = "Choose a template",
                style = typography.screenTitle,
                color = colors.textPrimary
            )

            Text(
                text = "Browse curated SparkCut templates, compare aspect ratios, and jump directly into creation with a polished starting point.",
                style = typography.body,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun TemplateGridCard(
    item: TemplateContract.TemplateItem,
    onClick: () -> Unit,
) {
    val spacing = SparkCutTheme.spacing
    val typography = SparkCutTheme.typography
    val colors = SparkCutTheme.colors

    SparkCutCard(
        modifier = Modifier.fillMaxWidth(),
        tone = when {
            item.isFeatured -> SparkCutCardTone.Accent
            else -> SparkCutCardTone.Elevated
        },
        onClick = onClick
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                SparkCutStatusChip(
                    text = item.categoryLabel,
                    tone = SparkCutStatusTone.Info
                )

                if (item.isFeatured) {
                    SparkCutStatusChip(
                        text = "Featured",
                        tone = SparkCutStatusTone.Accent
                    )
                }

                if (item.isPremium) {
                    SparkCutStatusChip(
                        text = "Premium",
                        tone = SparkCutStatusTone.Warning
                    )
                }
            }

            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = item.name,
                style = typography.sectionTitle,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = item.description,
                style = typography.body,
                color = colors.textSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                SparkCutStatusChip(
                    text = item.aspectRatioLabel,
                    tone = SparkCutStatusTone.Neutral
                )
                SparkCutStatusChip(
                    text = "${item.minMediaCount}-${item.maxMediaCount} assets",
                    tone = SparkCutStatusTone.Success
                )
            }

            if (item.tags.isNotEmpty()) {
                Text(
                    text = item.tags.joinToString(separator = " • "),
                    style = typography.meta,
                    color = colors.textMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            SparkCutPrimaryButton(
                text = "Use template",
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                size = SparkCutButtonSize.Medium
            )
        }
    }
}