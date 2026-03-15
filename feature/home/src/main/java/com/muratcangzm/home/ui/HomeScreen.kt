package com.muratcangzm.home.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muratcangzm.designsystem.button.SparkCutButtonSize
import com.muratcangzm.designsystem.button.SparkCutPrimaryButton
import com.muratcangzm.designsystem.card.SparkCutCard
import com.muratcangzm.designsystem.card.SparkCutCardTone
import com.muratcangzm.designsystem.chip.SparkCutStatusChip
import com.muratcangzm.designsystem.chip.SparkCutStatusTone
import com.muratcangzm.designsystem.component.SparkCutScaffold
import com.muratcangzm.designsystem.component.SparkCutTopBar
import com.muratcangzm.designsystem.theme.SparkCutTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    onOpenTemplateBrowser: () -> Unit,
    onOpenCreate: (templateId: String) -> Unit,
    onOpenCategory: (categoryName: String) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                HomeContract.Effect.NavigateToTemplateBrowser -> onOpenTemplateBrowser()
                is HomeContract.Effect.NavigateToCreate -> onOpenCreate(effect.templateId.value)
                is HomeContract.Effect.NavigateToCategory -> onOpenCategory(effect.category.name)
            }
        }
    }

    HomeScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun HomeScreenContent(
    state: HomeContract.State,
    onEvent: (HomeContract.Event) -> Unit,
) {
    val spacing = SparkCutTheme.spacing
    val colors = SparkCutTheme.colors

    SparkCutScaffold(
        topBar = {
            SparkCutTopBar(
                title = "SparkCut",
                subtitle = if (state.isLoading) {
                    "Preparing your creative workspace"
                } else {
                    "Create polished short videos faster"
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    item {
                        HomeHeroCard(
                            onBrowseTemplates = {
                                onEvent(HomeContract.Event.BrowseAllTemplatesClicked)
                            }
                        )
                    }

                    if (state.featuredTemplates.isNotEmpty()) {
                        item {
                            HomeSectionHeader(
                                title = "Featured templates",
                                subtitle = "Fast starting points for the most popular edit styles."
                            )
                        }

                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                                contentPadding = PaddingValues(end = spacing.xs),
                            ) {
                                items(
                                    items = state.featuredTemplates,
                                    key = { it.id.value }
                                ) { item ->
                                    FeaturedTemplateCard(
                                        item = item,
                                        onClick = {
                                            onEvent(
                                                HomeContract.Event.FeaturedTemplateClicked(item.id)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (state.categories.isNotEmpty()) {
                        item {
                            HomeSectionHeader(
                                title = "Categories",
                                subtitle = "Jump into a creative direction that already fits your content."
                            )
                        }

                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                                contentPadding = PaddingValues(vertical = spacing.xxs),
                            ) {
                                items(
                                    items = state.categories,
                                    key = { it.category.name }
                                ) { category ->
                                    androidx.compose.material3.FilterChip(
                                        selected = false,
                                        onClick = {
                                            onEvent(
                                                HomeContract.Event.CategoryClicked(category.category)
                                            )
                                        },
                                        label = {
                                            Text(category.label)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        HomeQuickBrowseCard(
                            featuredCount = state.featuredTemplates.size,
                            categoryCount = state.categories.size,
                            onBrowseTemplates = {
                                onEvent(HomeContract.Event.BrowseAllTemplatesClicked)
                            }
                        )
                    }

                    item {
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
private fun HomeHeroCard(
    onBrowseTemplates: () -> Unit,
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
                    text = "Fast workflow",
                    tone = SparkCutStatusTone.Info
                )
                SparkCutStatusChip(
                    text = "Template-based",
                    tone = SparkCutStatusTone.Accent
                )
                SparkCutStatusChip(
                    text = "Creator ready",
                    tone = SparkCutStatusTone.Success
                )
            }

            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }

            Text(
                text = "Make short videos fast",
                style = typography.display,
                color = colors.textPrimary
            )

            Text(
                text = "Pick a template, add your photos or clips, refine the edit, and export a polished reel without getting lost in complicated controls.",
                style = typography.body,
                color = colors.textSecondary
            )

            SparkCutPrimaryButton(
                text = "Browse templates",
                onClick = onBrowseTemplates,
                modifier = Modifier.fillMaxWidth(),
                size = SparkCutButtonSize.Large,
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Collections,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun HomeSectionHeader(
    title: String,
    subtitle: String,
) {
    val spacing = SparkCutTheme.spacing
    val typography = SparkCutTheme.typography
    val colors = SparkCutTheme.colors

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.xxs)
    ) {
        Text(
            text = title,
            style = typography.sectionTitle,
            color = colors.textPrimary
        )
        Text(
            text = subtitle,
            style = typography.body,
            color = colors.textSecondary
        )
    }
}

@Composable
private fun FeaturedTemplateCard(
    item: HomeContract.FeaturedTemplateItem,
    onClick: () -> Unit,
) {
    val spacing = SparkCutTheme.spacing
    val typography = SparkCutTheme.typography
    val colors = SparkCutTheme.colors

    SparkCutCard(
        modifier = Modifier.fillMaxWidth(0.86f),
        tone = SparkCutCardTone.Elevated,
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
                SparkCutStatusChip(
                    text = "Featured",
                    tone = SparkCutStatusTone.Accent
                )
            }

            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.VideoLibrary,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(26.dp)
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
                text = item.subtitle,
                style = typography.body,
                color = colors.textSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            SparkCutPrimaryButton(
                text = "Start with this template",
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                size = SparkCutButtonSize.Medium
            )
        }
    }
}

@Composable
private fun HomeQuickBrowseCard(
    featuredCount: Int,
    categoryCount: Int,
    onBrowseTemplates: () -> Unit,
) {
    val spacing = SparkCutTheme.spacing
    val typography = SparkCutTheme.typography
    val colors = SparkCutTheme.colors

    SparkCutCard(
        modifier = Modifier.fillMaxWidth(),
        tone = SparkCutCardTone.Elevated
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                SparkCutStatusChip(
                    text = "$featuredCount featured",
                    tone = SparkCutStatusTone.Info
                )
                SparkCutStatusChip(
                    text = "$categoryCount categories",
                    tone = SparkCutStatusTone.Neutral
                )
            }

            Text(
                text = "Explore the full template library",
                style = typography.sectionTitle,
                color = colors.textPrimary
            )

            Text(
                text = "Browse all available templates when you want more styles, more categories, and more control over how your next edit begins.",
                style = typography.body,
                color = colors.textSecondary
            )

            SparkCutPrimaryButton(
                text = "Open template library",
                onClick = onBrowseTemplates,
                modifier = Modifier.fillMaxWidth(),
                size = SparkCutButtonSize.Medium
            )
        }
    }
}