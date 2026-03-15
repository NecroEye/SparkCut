package com.muratcangzm.projects.ui

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muratcangzm.designsystem.banner.SparkCutValidationBanner
import com.muratcangzm.designsystem.banner.SparkCutValidationBannerSeverity
import com.muratcangzm.designsystem.button.SparkCutButtonSize
import com.muratcangzm.designsystem.button.SparkCutPrimaryButton
import com.muratcangzm.designsystem.button.SparkCutSecondaryButton
import com.muratcangzm.designsystem.card.SparkCutCard
import com.muratcangzm.designsystem.card.SparkCutCardTone
import com.muratcangzm.designsystem.chip.SparkCutStatusChip
import com.muratcangzm.designsystem.chip.SparkCutStatusTone
import com.muratcangzm.designsystem.component.SparkCutScaffold
import com.muratcangzm.designsystem.component.SparkCutTopBar
import com.muratcangzm.designsystem.theme.SparkCutTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProjectsScreen(
    onOpenProject: (String) -> Unit,
    viewModel: ProjectsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ProjectsContract.Effect.NavigateEditor -> onOpenProject(effect.projectId)
                is ProjectsContract.Effect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    ProjectsScreenContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun ProjectsScreenContent(
    state: ProjectsContract.State,
    snackbarHostState: SnackbarHostState,
    onEvent: (ProjectsContract.Event) -> Unit,
) {
    val spacing = SparkCutTheme.spacing
    val colors = SparkCutTheme.colors

    SparkCutScaffold(
        snackbarHostState = snackbarHostState,
        topBar = {
            SparkCutTopBar(
                title = "Projects",
                subtitle = if (state.items.isEmpty()) {
                    "Saved drafts and exported work"
                } else {
                    "${state.items.size} project${if (state.items.size == 1) "" else "s"}"
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

            !state.errorMessage.isNullOrBlank() -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    item {
                        SparkCutValidationBanner(
                            title = "Projects unavailable",
                            message = state.errorMessage,
                            severity = SparkCutValidationBannerSeverity.Error
                        )
                    }
                }
            }

            state.items.isEmpty() -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    item {
                        EmptyProjectsCard()
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    item {
                        ProjectsSummaryCard(
                            itemCount = state.items.size,
                            lastActiveProjectId = state.lastActiveProjectId
                        )
                    }

                    items(
                        items = state.items,
                        key = { it.id }
                    ) { item ->
                        ProjectListCard(
                            item = item,
                            onOpen = {
                                onEvent(ProjectsContract.Event.OpenProject(item.id))
                            },
                            onDelete = {
                                onEvent(ProjectsContract.Event.DeleteProject(item.id))
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
private fun ProjectsSummaryCard(
    itemCount: Int,
    lastActiveProjectId: String?,
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
                    text = "$itemCount saved",
                    tone = SparkCutStatusTone.Info
                )
                SparkCutStatusChip(
                    text = if (lastActiveProjectId != null) "Resume available" else "No recent focus",
                    tone = if (lastActiveProjectId != null) {
                        SparkCutStatusTone.Success
                    } else {
                        SparkCutStatusTone.Neutral
                    }
                )
            }

            Text(
                text = "Continue your work",
                style = typography.screenTitle,
                color = colors.textPrimary
            )

            Text(
                text = "SparkCut keeps your project sessions ready so you can jump back into editing, exporting, or revising drafts without losing momentum.",
                style = typography.body,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun EmptyProjectsCard() {
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
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = "No saved projects yet",
                style = typography.sectionTitle,
                color = colors.textPrimary
            )

            Text(
                text = "Your drafts, active editing sessions, and exported projects will appear here once you start creating.",
                style = typography.body,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun ProjectListCard(
    item: ProjectsContract.ProjectItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val spacing = SparkCutTheme.spacing
    val typography = SparkCutTheme.typography
    val colors = SparkCutTheme.colors

    SparkCutCard(
        modifier = Modifier.fillMaxWidth(),
        tone = if (item.isLastActive) {
            SparkCutCardTone.Focused
        } else {
            SparkCutCardTone.Elevated
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(spacing.xxs)
                ) {
                    Text(
                        text = item.name,
                        style = typography.sectionTitle,
                        color = colors.textPrimary
                    )

                    Text(
                        text = "Template ${item.templateId}",
                        style = typography.meta,
                        color = colors.textMuted
                    )
                }

                if (item.isLastActive) {
                    SparkCutStatusChip(
                        text = "Last active",
                        tone = SparkCutStatusTone.Accent
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                SparkCutStatusChip(
                    text = item.statusLabel,
                    tone = projectStatusTone(item.statusLabel)
                )
                SparkCutStatusChip(
                    text = item.aspectRatioLabel,
                    tone = SparkCutStatusTone.Neutral
                )
                SparkCutStatusChip(
                    text = "${item.mediaCount} media",
                    tone = SparkCutStatusTone.Info
                )
            }

            ProjectMetaRow(
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = "Updated",
                value = item.updatedAtLabel
            )

            ProjectMetaRow(
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.VideoLibrary,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = "Project state",
                value = if (item.isLastActive) {
                    "${item.statusLabel} • Continue where you left off"
                } else {
                    item.statusLabel
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                SparkCutPrimaryButton(
                    text = if (item.isLastActive) {
                        "Resume"
                    } else {
                        "Open"
                    },
                    onClick = onOpen,
                    modifier = Modifier.weight(1f),
                    size = SparkCutButtonSize.Medium,
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                SparkCutSecondaryButton(
                    text = "Delete",
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    size = SparkCutButtonSize.Medium,
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ProjectMetaRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
) {
    val spacing = SparkCutTheme.spacing
    val typography = SparkCutTheme.typography
    val colors = SparkCutTheme.colors

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        icon()

        Text(
            text = label,
            style = typography.meta,
            color = colors.textMuted
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = value,
            style = typography.meta,
            color = colors.textSecondary
        )
    }
}

private fun projectStatusTone(statusLabel: String): SparkCutStatusTone {
    return when (statusLabel) {
        "Draft" -> SparkCutStatusTone.Warning
        "Ready" -> SparkCutStatusTone.Success
        "Exporting" -> SparkCutStatusTone.Info
        "Exported" -> SparkCutStatusTone.Success
        "Failed" -> SparkCutStatusTone.Error
        else -> SparkCutStatusTone.Neutral
    }
}