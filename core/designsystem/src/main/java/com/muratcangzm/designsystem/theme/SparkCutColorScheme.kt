package com.muratcangzm.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class SparkCutColorScheme(
    val background: Color,
    val backgroundAlt: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceFocused: Color,
    val surfaceAccent: Color,

    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,

    val strokeSoft: Color,
    val strokeStrong: Color,

    val primary: Color,
    val primaryPressed: Color,
    val primaryMuted: Color,

    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color,

    val successContainer: Color,
    val warningContainer: Color,
    val errorContainer: Color,
    val infoContainer: Color
)