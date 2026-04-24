package com.muratcangzm.settings.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

private object SettingsColors {
    val Background = Color(0xFF0E1117)
    val Surface = Color(0xFF161B22)
    val SurfaceElevated = Color(0xFF1C2129)
    val Accent = Color(0xFF00D4AA)
    val AccentDim = Color(0xFF00A888)
    val TextPrimary = Color(0xFFF0F6FC)
    val TextSecondary = Color(0xFF8B949E)
    val TextMuted = Color(0xFF484F58)
    val Border = Color(0xFF21262D)
    val Destructive = Color(0xFFF85149)
    val GradientStart = Color(0xFF00D4AA)
    val GradientEnd = Color(0xFF7F73FF)
}

private object SettingsLayout {
    val HorizontalPadding = 20.dp
    val RowVerticalPadding = 12.dp
    val ItemIconSize = 40.dp
    val ItemIconCornerRadius = 12.dp
    val ItemIconInnerSize = 20.dp
    val ItemIconSpacing = 14.dp
    val OptionRowStartIndent = ItemIconSize + ItemIconSpacing
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SettingsContract.Effect.NavigateBack -> onBack()
                is SettingsContract.Effect.ShowMessage ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    SettingsScreenContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun SettingsScreenContent(
    state: SettingsContract.State,
    snackbarHostState: SnackbarHostState,
    onEvent: (SettingsContract.Event) -> Unit,
) {
    Scaffold(
        containerColor = SettingsColors.Background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsTopBar(onBack = { onEvent(SettingsContract.Event.BackClicked) })

            Spacer(modifier = Modifier.height(4.dp))

            SettingsSectionHeader(title = "Export")

            SettingsOptionPicker(
                icon = Icons.Outlined.HighQuality,
                title = "Export Quality",
                subtitle = state.selectedExportQuality.label,
                options = SettingsContract.ExportQuality.entries.map { it.label },
                selectedIndex = SettingsContract.ExportQuality.entries.indexOf(state.selectedExportQuality),
                onOptionSelected = { index ->
                    onEvent(
                        SettingsContract.Event.ExportQualitySelected(
                            SettingsContract.ExportQuality.entries[index]
                        )
                    )
                },
            )

            SettingsOptionPicker(
                icon = Icons.Outlined.Videocam,
                title = "Frame Rate",
                subtitle = state.selectedDefaultFrameRate.label,
                options = SettingsContract.FrameRate.entries.map { it.label },
                selectedIndex = SettingsContract.FrameRate.entries.indexOf(state.selectedDefaultFrameRate),
                onOptionSelected = { index ->
                    onEvent(
                        SettingsContract.Event.FrameRateSelected(
                            SettingsContract.FrameRate.entries[index]
                        )
                    )
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsSectionHeader(title = "Editor")

            SettingsToggleRow(
                icon = Icons.Outlined.Save,
                title = "Auto-Save",
                subtitle = if (state.autoSaveEnabled) "Every ${state.autoSaveIntervalSeconds}s" else "Disabled",
                checked = state.autoSaveEnabled,
                onToggle = { onEvent(SettingsContract.Event.AutoSaveToggled(it)) },
            )

            SettingsToggleRow(
                icon = Icons.Outlined.Speed,
                title = "Hardware Acceleration",
                subtitle = if (state.hardwareAccelerationEnabled) "GPU rendering enabled" else "Software rendering",
                checked = state.hardwareAccelerationEnabled,
                onToggle = { onEvent(SettingsContract.Event.HardwareAccelerationToggled(it)) },
            )

            SettingsToggleRow(
                icon = Icons.Outlined.Tune,
                title = "Watermark",
                subtitle = if (state.showWatermark) "SparkCut watermark on exports" else "No watermark",
                checked = state.showWatermark,
                onToggle = { onEvent(SettingsContract.Event.WatermarkToggled(it)) },
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsSectionHeader(title = "Storage")

            SettingsActionRow(
                icon = Icons.Outlined.DeleteSweep,
                title = "Clear Cache",
                subtitle = state.cacheUsageLabel,
                isLoading = state.isClearingCache,
                onClick = { onEvent(SettingsContract.Event.ClearCacheClicked) },
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsSectionHeader(title = "General")

            SettingsActionRow(
                icon = Icons.Outlined.RestartAlt,
                title = "Reset to Defaults",
                subtitle = "Restore all settings to original values",
                isDestructive = true,
                onClick = { onEvent(SettingsContract.Event.ResetToDefaultsClicked) },
            )

            Spacer(modifier = Modifier.height(20.dp))

            SettingsFooter(appVersion = state.appVersion)

            Spacer(
                modifier = Modifier
                    .navigationBarsPadding()
                    .height(20.dp),
            )
        }
    }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = SettingsLayout.HorizontalPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(SettingsLayout.ItemIconSize)
                .clip(RoundedCornerShape(SettingsLayout.ItemIconCornerRadius))
                .background(SettingsColors.Surface)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = SettingsColors.TextPrimary,
                modifier = Modifier.size(SettingsLayout.ItemIconInnerSize),
            )
        }

        Spacer(modifier = Modifier.width(SettingsLayout.ItemIconSpacing))

        Text(
            text = "Settings",
            color = SettingsColors.TextPrimary,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = SettingsColors.TextMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(
            horizontal = SettingsLayout.HorizontalPadding,
            vertical = 8.dp,
        ),
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(
                horizontal = SettingsLayout.HorizontalPadding,
                vertical = SettingsLayout.RowVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(SettingsLayout.ItemIconSize)
                .clip(RoundedCornerShape(SettingsLayout.ItemIconCornerRadius))
                .background(SettingsColors.Surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SettingsColors.Accent,
                modifier = Modifier.size(SettingsLayout.ItemIconInnerSize),
            )
        }

        Spacer(modifier = Modifier.width(SettingsLayout.ItemIconSpacing))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = SettingsColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                color = SettingsColors.TextSecondary,
                fontSize = 12.sp,
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SettingsColors.Background,
                checkedTrackColor = SettingsColors.Accent,
                uncheckedThumbColor = SettingsColors.TextMuted,
                uncheckedTrackColor = SettingsColors.Surface,
                uncheckedBorderColor = SettingsColors.Border,
            ),
        )
    }
}

@Composable
private fun SettingsOptionPicker(
    icon: ImageVector,
    title: String,
    subtitle: String,
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = SettingsLayout.HorizontalPadding,
                vertical = 8.dp,
            ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(SettingsLayout.ItemIconSize)
                    .clip(RoundedCornerShape(SettingsLayout.ItemIconCornerRadius))
                    .background(SettingsColors.Surface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SettingsColors.Accent,
                    modifier = Modifier.size(SettingsLayout.ItemIconInnerSize),
                )
            }

            Spacer(modifier = Modifier.width(SettingsLayout.ItemIconSpacing))

            Column {
                Text(
                    text = title,
                    color = SettingsColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = subtitle,
                    color = SettingsColors.TextSecondary,
                    fontSize = 12.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .padding(start = SettingsLayout.OptionRowStartIndent)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEachIndexed { index, label ->
                val isSelected = index == selectedIndex

                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) SettingsColors.Accent else SettingsColors.Surface,
                    animationSpec = tween(200),
                    label = "optionBg",
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) SettingsColors.Background else SettingsColors.TextSecondary,
                    animationSpec = tween(200),
                    label = "optionText",
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) SettingsColors.Accent else SettingsColors.Border,
                    animationSpec = tween(200),
                    label = "optionBorder",
                )

                Text(
                    text = label,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                        .clickable { onOptionSelected(index) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isLoading: Boolean = false,
    isDestructive: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(
                horizontal = SettingsLayout.HorizontalPadding,
                vertical = SettingsLayout.RowVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(SettingsLayout.ItemIconSize)
                .clip(RoundedCornerShape(SettingsLayout.ItemIconCornerRadius))
                .background(SettingsColors.Surface),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = SettingsColors.Accent,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDestructive) SettingsColors.Destructive else SettingsColors.Accent,
                    modifier = Modifier.size(SettingsLayout.ItemIconInnerSize),
                )
            }
        }

        Spacer(modifier = Modifier.width(SettingsLayout.ItemIconSpacing))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isDestructive) SettingsColors.Destructive else SettingsColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                color = SettingsColors.TextSecondary,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun SettingsFooter(appVersion: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SettingsLayout.HorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(SettingsColors.GradientStart, SettingsColors.GradientEnd),
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Cached,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "SparkCut",
            color = SettingsColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Version $appVersion",
            color = SettingsColors.TextMuted,
            fontSize = 12.sp,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Made with \u2764 by Muratcan Gözüm",
            color = SettingsColors.TextMuted,
            fontSize = 11.sp,
        )
    }
}
