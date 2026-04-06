package com.muratcangzm.home.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

private object HomeColors {
    val Background = Color(0xFF0E1117)
    val Surface = Color(0xFF161B22)
    val SurfaceElevated = Color(0xFF1C2129)
    val Accent = Color(0xFF00D4AA)
    val AccentAlt = Color(0xFF00E5C3)
    val TextPrimary = Color(0xFFF0F6FC)
    val TextSecondary = Color(0xFF8B949E)
    val TextMuted = Color(0xFF484F58)
    val Border = Color(0xFF21262D)
    val GradientStart = Color(0xFF00D4AA)
    val GradientEnd = Color(0xFF7F73FF)
}

@Composable
fun HomeScreen(
    onOpenEditor: (mediaUris: List<String>) -> Unit,
    onOpenProject: (projectId: String) -> Unit,
    onOpenSettings: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20),
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.onMediaSelected(uris.map { it.toString() })
        }
    }

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HomeContract.Effect.NavigateToEditor -> onOpenEditor(effect.mediaUris)
                is HomeContract.Effect.NavigateToExistingProject -> onOpenProject(effect.projectId)
            }
        }
    }

    HomeScreenContent(
        state = state,
        onNewProject = {
            mediaPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
            )
        },
        onOpenProject = { projectId ->
            viewModel.onEvent(HomeContract.Event.OpenProjectClicked(projectId))
        },
        onOpenSettings = onOpenSettings,
    )
}

@Composable
private fun HomeScreenContent(
    state: HomeContract.State,
    onNewProject: () -> Unit,
    onOpenProject: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeColors.Background),
    ) {
        BackgroundGlow()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
        ) {
            item { TopHeader(onOpenSettings = onOpenSettings) }
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item { HeroSection(onNewProject = onNewProject) }
            item { Spacer(modifier = Modifier.height(32.dp)) }
            item { QuickActions(onNewProject = onNewProject) }

            if (state.recentProjects.isNotEmpty()) {
                item { Spacer(modifier = Modifier.height(32.dp)) }
                item {
                    SectionHeader(
                        title = "Recent Projects",
                        subtitle = "Continue where you left off",
                    )
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = state.recentProjects,
                            key = { it.projectId },
                        ) { project ->
                            RecentProjectCard(
                                project = project,
                                onClick = { onOpenProject(project.projectId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackgroundGlow() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            HomeColors.Accent.copy(alpha = 0.06f),
                            Color.Transparent,
                        ),
                        radius = 400f,
                    ),
                ),
        )
    }
}

@Composable
private fun TopHeader(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(HomeColors.GradientStart, HomeColors.GradientEnd),
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = "SparkCut",
                color = HomeColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Video Editor",
                color = HomeColors.TextSecondary,
                fontSize = 13.sp,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Settings",
                tint = HomeColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun HeroSection(onNewProject: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            HomeColors.SurfaceElevated,
                            HomeColors.Surface,
                        ),
                    ),
                )
                .border(1.dp, HomeColors.Border, RoundedCornerShape(24.dp))
                .padding(24.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HeroPill("Video Editor")
                HeroPill("AI Captions")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Create stunning\nvideos in minutes",
                color = HomeColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 34.sp,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Pick your photos and videos, edit with timeline, add AI-powered captions",
                color = HomeColors.TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onNewProject,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HomeColors.Accent,
                    contentColor = Color(0xFF0A0F14),
                ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "New Project",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun HeroPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(HomeColors.Accent.copy(alpha = 0.1f))
            .border(1.dp, HomeColors.Accent.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            color = HomeColors.Accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun QuickActions(onNewProject: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader(
            title = "Quick Start",
            subtitle = "Choose your media type",
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuickActionCard(
                icon = Icons.Outlined.VideoLibrary,
                title = "Video",
                subtitle = "Edit video clips",
                accentColor = HomeColors.Accent,
                modifier = Modifier.weight(1f),
                onClick = onNewProject,
            )
            QuickActionCard(
                icon = Icons.Outlined.Image,
                title = "Photo",
                subtitle = "Create slideshow",
                accentColor = Color(0xFF7F73FF),
                modifier = Modifier.weight(1f),
                onClick = onNewProject,
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(HomeColors.Surface)
            .border(1.dp, HomeColors.Border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = title,
            color = HomeColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Text(
            text = subtitle,
            color = HomeColors.TextSecondary,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
) {
    Column {
        Text(
            text = title,
            color = HomeColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = subtitle,
            color = HomeColors.TextSecondary,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun RecentProjectCard(
    project: HomeContract.RecentProjectItem,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(HomeColors.Surface)
            .border(1.dp, HomeColors.Border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .background(HomeColors.SurfaceElevated),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.MovieCreation,
                contentDescription = null,
                tint = HomeColors.TextMuted,
                modifier = Modifier.size(28.dp),
            )
        }

        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = project.name,
                color = HomeColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${project.mediaCount} clips \u2022 ${project.lastEditedLabel}",
                color = HomeColors.TextMuted,
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
    }
}
