package com.muratcangzm.template.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                is TemplateContract.Effect.NavigateToCreate -> onOpenCreate(effect.templateId.value)
                TemplateContract.Effect.NavigateBack -> onBack()
            }
        }
    }

    TemplateScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateScreenContent(
    state: TemplateContract.State,
    onEvent: (TemplateContract.Event) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Templates") },
                navigationIcon = {
                    IconButton(onClick = { onEvent(TemplateContract.Event.BackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(),
            )
        },
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 180.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(
                span = { GridItemSpan(maxLineSpan) }
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = {
                        onEvent(TemplateContract.Event.QueryChanged(it))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                        )
                    },
                    label = { Text("Search templates") },
                    singleLine = true,
                )
            }

            item(
                span = { GridItemSpan(maxLineSpan) }
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(state.categories.size) { index ->
                        val category = state.categories[index]
                        FilterChip(
                            selected = category.isSelected,
                            onClick = {
                                onEvent(
                                    TemplateContract.Event.CategorySelected(category.category)
                                )
                            },
                            label = { Text(category.label) },
                        )
                    }
                }
            }

            item(
                span = { GridItemSpan(maxLineSpan) }
            ) {
                Text(
                    text = state.resultCountLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (state.isEmpty) {
                item(
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    ) {
                        Text(
                            text = "No templates found. Try another keyword or category.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            items(
                items = state.templates,
                key = { it.id.value },
            ) { item ->
                TemplateCard(
                    item = item,
                    onClick = {
                        onEvent(TemplateContract.Event.TemplateClicked(item.id))
                    },
                )
            }
        }
    }
}

@Composable
private fun TemplateCard(
    item: TemplateContract.TemplateItem,
    onClick: () -> Unit,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(item.categoryLabel) },
                )

                if (item.isFeatured) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text("Featured") },
                    )
                }

                if (item.isPremium) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text("Premium") },
                    )
                }
            }

            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = "${item.minMediaCount}-${item.maxMediaCount} assets • ${item.aspectRatioLabel}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            if (item.tags.isNotEmpty()) {
                Text(
                    text = item.tags.joinToString(separator = " • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}