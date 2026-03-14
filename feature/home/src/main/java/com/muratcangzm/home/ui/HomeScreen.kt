package com.muratcangzm.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    state: HomeContract.State,
    onEvent: (HomeContract.Event) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SparkCut") },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                        )

                        Text(
                            text = "Make short videos fast",
                            style = MaterialTheme.typography.headlineSmall,
                        )

                        Text(
                            text = "Pick a template, add your photos or clips, and export a polished reel in minutes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Button(
                            onClick = {
                                onEvent(HomeContract.Event.BrowseAllTemplatesClicked)
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Collections,
                                contentDescription = null,
                            )
                            Text(
                                text = "Browse templates",
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }

            item {
                SectionTitle(
                    title = "Featured",
                    subtitle = "Best starting points for fast edits",
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.featuredTemplates.size) { index ->
                        val item = state.featuredTemplates[index]
                        FeaturedTemplateCard(
                            item = item,
                            onClick = {
                                onEvent(
                                    HomeContract.Event.FeaturedTemplateClicked(item.id)
                                )
                            },
                        )
                    }
                }
            }

            item {
                SectionTitle(
                    title = "Categories",
                    subtitle = "Jump into a ready-made content style",
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.categories.size) { index ->
                        val category = state.categories[index]
                        AssistChip(
                            onClick = {
                                onEvent(HomeContract.Event.CategoryClicked(category.category))
                            },
                            label = { Text(category.label) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String,
) {
    androidx.compose.foundation.layout.Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FeaturedTemplateCard(
    item: HomeContract.FeaturedTemplateItem,
    onClick: () -> Unit,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(0.88f),
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.VideoLibrary,
                contentDescription = null,
            )

            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(item.categoryLabel) },
            )
        }
    }
}