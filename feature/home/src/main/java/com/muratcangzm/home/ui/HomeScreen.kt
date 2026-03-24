package com.muratcangzm.home.ui

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.MovieFilter
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muratcangzm.designsystem.theme.SparkCutTheme
import com.muratcangzm.model.template.TemplateCategory
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
    val colors = SparkCutTheme.colors
    val spacing = SparkCutTheme.spacing

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        HomeBackgroundGlow()

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = colors.primary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(
                    start = spacing.md,
                    top = spacing.sm,
                    end = spacing.md,
                    bottom = spacing.xxl
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    HomeTopHeader()
                }

                item {
                    HomeHeroSection(
                        featuredCount = state.featuredTemplates.size,
                        categoryCount = state.categories.size,
                        onBrowseTemplates = {
                            onEvent(HomeContract.Event.BrowseAllTemplatesClicked)
                        }
                    )
                }

                if (state.featuredTemplates.isNotEmpty()) {
                    item {
                        HomeSectionHeader(
                            eyebrow = "FEATURED",
                            title = "Best starting points",
                            subtitle = "Fast, stylish templates that get you to a polished result quickly."
                        )
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(end = 6.dp)
                        ) {
                            items(
                                items = state.featuredTemplates,
                                key = { it.id.value }
                            ) { item ->
                                FeaturedTemplateCard(
                                    item = item,
                                    onClick = {
                                        onEvent(HomeContract.Event.FeaturedTemplateClicked(item.id))
                                    }
                                )
                            }
                        }
                    }
                }

                if (state.categories.isNotEmpty()) {
                    item {
                        HomeSectionHeader(
                            eyebrow = "CATEGORIES",
                            title = "Browse by vibe",
                            subtitle = "Jump into a content style that already matches the mood you want."
                        )
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(end = 6.dp)
                        ) {
                            items(
                                items = state.categories,
                                key = { it.category.name }
                            ) { item ->
                                CategoryPillCard(
                                    item = item,
                                    onClick = {
                                        onEvent(HomeContract.Event.CategoryClicked(item.category))
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    HomeBottomCallout(
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

@Composable
private fun HomeBackgroundGlow() {
    val colors = SparkCutTheme.colors

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = (-60).dp, y = (-30).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colors.primary.copy(alpha = 0.16f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = 90.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF8B5CF6).copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun HomeTopHeader() {
    val colors = SparkCutTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BrandBadge()

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "SparkCut",
                style = SparkCutTheme.typography.sectionTitle,
                color = colors.textPrimary
            )
            Text(
                text = "Template-first short video maker",
                style = SparkCutTheme.typography.meta,
                color = colors.textMuted
            )
        }

        MinimalChip(
            text = "BETA",
            textColor = colors.primary,
            containerColor = colors.primaryMuted,
            borderColor = colors.primary.copy(alpha = 0.25f)
        )
    }
}

@Composable
private fun BrandBadge() {
    val colors = SparkCutTheme.colors

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        colors.primary.copy(alpha = 0.95f),
                        Color(0xFF8B5CF6).copy(alpha = 0.90f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = colors.background,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun HomeHeroSection(
    featuredCount: Int,
    categoryCount: Int,
    onBrowseTemplates: () -> Unit,
) {
    val colors = SparkCutTheme.colors

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = colors.strokeStrong.copy(alpha = 0.65f)
        )
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            colors.surfaceFocused,
                            Color(0xFF171D31),
                            Color(0xFF101722)
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(140.dp)
                    .offset(x = 30.dp, y = (-20).dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.primary.copy(alpha = 0.20f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroPill("Cool edits")
                    HeroPill("Fast exports")
                    HeroPill("Mobile flow")
                }

                Text(
                    text = "Cut faster.\nLook sharper.",
                    style = SparkCutTheme.typography.display.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = colors.textPrimary
                )

                Text(
                    text = "SparkCut helps you turn raw clips into stylish short videos without getting stuck in a heavy editor.",
                    style = SparkCutTheme.typography.body,
                    color = colors.textSecondary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBadge("$featuredCount featured")
                    StatBadge("$categoryCount categories")
                    StatBadge("1-tap start")
                }

                Button(
                    onClick = onBrowseTemplates,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.background
                    ),
                    contentPadding = PaddingValues(
                        horizontal = 18.dp,
                        vertical = 16.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Collections,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Browse templates",
                        style = SparkCutTheme.typography.button
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroPill(text: String) {
    val colors = SparkCutTheme.colors

    MinimalChip(
        text = text,
        textColor = colors.textSecondary,
        containerColor = colors.surface.copy(alpha = 0.80f),
        borderColor = colors.strokeSoft.copy(alpha = 0.80f)
    )
}

@Composable
private fun StatBadge(text: String) {
    val colors = SparkCutTheme.colors

    MinimalChip(
        text = text,
        textColor = colors.textPrimary,
        containerColor = colors.surfaceElevated.copy(alpha = 0.95f),
        borderColor = colors.strokeStrong.copy(alpha = 0.65f)
    )
}

@Composable
private fun MinimalChip(
    text: String,
    textColor: Color,
    containerColor: Color,
    borderColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = borderColor
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 8.dp
            ),
            style = SparkCutTheme.typography.label,
            color = textColor
        )
    }
}

@Composable
private fun HomeSectionHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
) {
    val colors = SparkCutTheme.colors

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = eyebrow,
            style = SparkCutTheme.typography.label,
            color = colors.primary
        )
        Text(
            text = title,
            style = SparkCutTheme.typography.sectionTitle,
            color = colors.textPrimary
        )
        Text(
            text = subtitle,
            style = SparkCutTheme.typography.body,
            color = colors.textSecondary
        )
    }
}

@Composable
private fun FeaturedTemplateCard(
    item: HomeContract.FeaturedTemplateItem,
    onClick: () -> Unit,
) {
    val colors = SparkCutTheme.colors

    Surface(
        modifier = Modifier.width(292.dp),
        shape = RoundedCornerShape(28.dp),
        color = colors.surfaceElevated,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = colors.strokeSoft.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                categoryAccent(item.categoryLabel).copy(alpha = 0.34f),
                                colors.surfaceFocused
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MinimalChip(
                            text = item.categoryLabel,
                            textColor = colors.textPrimary,
                            containerColor = colors.background.copy(alpha = 0.35f),
                            borderColor = colors.strokeStrong.copy(alpha = 0.45f)
                        )
                        MinimalChip(
                            text = "Featured",
                            textColor = colors.primary,
                            containerColor = colors.primaryMuted.copy(alpha = 0.95f),
                            borderColor = colors.primary.copy(alpha = 0.25f)
                        )
                    }

                    Icon(
                        imageVector = Icons.Outlined.VideoLibrary,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(26.dp)
                    )

                    Text(
                        text = item.name,
                        style = SparkCutTheme.typography.sectionTitle,
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = item.subtitle,
                style = SparkCutTheme.typography.body,
                color = colors.textSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.surfaceFocused,
                    contentColor = colors.textPrimary
                ),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 14.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Use template",
                    style = SparkCutTheme.typography.button
                )
            }
        }
    }
}

@Composable
private fun CategoryPillCard(
    item: HomeContract.CategoryItem,
    onClick: () -> Unit,
) {
    val colors = SparkCutTheme.colors
    val accent = categoryAccent(item.category)

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = accent.copy(alpha = 0.28f)
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 12.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon(item.category),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = item.label,
                style = SparkCutTheme.typography.bodyStrong,
                color = colors.textPrimary
            )
        }
    }
}

@Composable
private fun HomeBottomCallout(
    onBrowseTemplates: () -> Unit,
) {
    val colors = SparkCutTheme.colors

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = colors.strokeSoft.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MinimalChip(
                    text = "Library",
                    textColor = colors.primary,
                    containerColor = colors.primaryMuted.copy(alpha = 0.95f),
                    borderColor = colors.primary.copy(alpha = 0.24f)
                )
                MinimalChip(
                    text = "More styles",
                    textColor = colors.textSecondary,
                    containerColor = colors.surfaceElevated,
                    borderColor = colors.strokeSoft
                )
            }

            Text(
                text = "Want more directions?",
                style = SparkCutTheme.typography.sectionTitle,
                color = colors.textPrimary
            )

            Text(
                text = "Open the full template browser to compare styles, categories and creative moods before starting your next edit.",
                style = SparkCutTheme.typography.body,
                color = colors.textSecondary
            )

            TextButton(
                onClick = onBrowseTemplates,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colors.primary
                )
            ) {
                Text(
                    text = "Open template library",
                    style = SparkCutTheme.typography.button
                )
            }
        }
    }
}

private fun categoryIcon(category: TemplateCategory): ImageVector {
    return when (category) {
        TemplateCategory.TRENDING -> Icons.Outlined.TrendingUp
        TemplateCategory.PARTY -> Icons.Outlined.Whatshot
        TemplateCategory.LOVE -> Icons.Outlined.FavoriteBorder
        TemplateCategory.TRAVEL -> Icons.Outlined.FlightTakeoff
        TemplateCategory.FITNESS -> Icons.Outlined.FitnessCenter
        TemplateCategory.PROMO -> Icons.Outlined.Campaign
        TemplateCategory.GLITCH -> Icons.Outlined.FlashOn
        TemplateCategory.BIRTHDAY -> Icons.Outlined.Cake
        TemplateCategory.MEMORIES -> Icons.Outlined.PhotoLibrary
        TemplateCategory.BUSINESS -> Icons.Outlined.BusinessCenter
        TemplateCategory.MINIMAL -> Icons.Outlined.Workspaces
        TemplateCategory.CINEMATIC -> Icons.Outlined.MovieFilter
    }
}

private fun categoryAccent(category: TemplateCategory): Color {
    return when (category) {
        TemplateCategory.TRENDING -> Color(0xFF5AA9FF)
        TemplateCategory.PARTY -> Color(0xFFFF8A5B)
        TemplateCategory.LOVE -> Color(0xFFFF6FAE)
        TemplateCategory.TRAVEL -> Color(0xFF55D6BE)
        TemplateCategory.FITNESS -> Color(0xFFFFB347)
        TemplateCategory.PROMO -> Color(0xFF8B5CF6)
        TemplateCategory.GLITCH -> Color(0xFF00C2FF)
        TemplateCategory.BIRTHDAY -> Color(0xFFFF7B7B)
        TemplateCategory.MEMORIES -> Color(0xFF7CC6FE)
        TemplateCategory.BUSINESS -> Color(0xFF94A3B8)
        TemplateCategory.MINIMAL -> Color(0xFFE2E8F0)
        TemplateCategory.CINEMATIC -> Color(0xFFB388FF)
    }
}

private fun categoryAccent(categoryLabel: String): Color {
    return when (categoryLabel) {
        "Trending" -> Color(0xFF5AA9FF)
        "Party" -> Color(0xFFFF8A5B)
        "Love" -> Color(0xFFFF6FAE)
        "Travel" -> Color(0xFF55D6BE)
        "Fitness" -> Color(0xFFFFB347)
        "Promo" -> Color(0xFF8B5CF6)
        "Glitch" -> Color(0xFF00C2FF)
        "Birthday" -> Color(0xFFFF7B7B)
        "Memories" -> Color(0xFF7CC6FE)
        "Business" -> Color(0xFF94A3B8)
        "Minimal" -> Color(0xFFE2E8F0)
        "Cinematic" -> Color(0xFFB388FF)
        else -> Color(0xFF5AA9FF)
    }
}