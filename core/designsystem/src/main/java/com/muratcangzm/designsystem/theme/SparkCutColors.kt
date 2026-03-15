package com.muratcangzm.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

internal val SparkCutInk950 = Color(0xFF0B1016)
internal val SparkCutInk900 = Color(0xFF101722)
internal val SparkCutInk850 = Color(0xFF141D2A)
internal val SparkCutInk800 = Color(0xFF182435)
internal val SparkCutInk750 = Color(0xFF1D2C40)

internal val SparkCutIce50 = Color(0xFFF5F7FA)
internal val SparkCutIce200 = Color(0xFFC7D0DB)
internal val SparkCutSlate400 = Color(0xFF8B98A9)
internal val SparkCutSlate600 = Color(0xFF223247)
internal val SparkCutSlate700 = Color(0xFF35506F)

internal val SparkCutBlue400 = Color(0xFF52B6FF)
internal val SparkCutBlue300 = Color(0xFF73C7FF)
internal val SparkCutBlue900 = Color(0xFF16324C)

internal val SparkCutViolet400 = Color(0xFF9A8CFF)
internal val SparkCutViolet900 = Color(0xFF241F42)

internal val SparkCutMint400 = Color(0xFF35D07F)
internal val SparkCutMint300 = Color(0xFF62E39B)
internal val SparkCutMint900 = Color(0xFF11271C)

internal val SparkCutAmber400 = Color(0xFFF0B44C)
internal val SparkCutAmber900 = Color(0xFF2A2112)

internal val SparkCutCoral400 = Color(0xFFFF6B6B)
internal val SparkCutCoral900 = Color(0xFF30181A)

internal val SparkCutDarkColorScheme = SparkCutColorScheme(
    background = SparkCutInk950,
    backgroundAlt = SparkCutInk900,
    surface = SparkCutInk850,
    surfaceElevated = SparkCutInk800,
    surfaceFocused = SparkCutInk750,
    surfaceAccent = SparkCutBlue900,

    textPrimary = SparkCutIce50,
    textSecondary = SparkCutIce200,
    textMuted = SparkCutSlate400,

    strokeSoft = SparkCutSlate600,
    strokeStrong = SparkCutSlate700,

    primary = SparkCutBlue400,
    primaryPressed = Color(0xFF379FEF),
    primaryMuted = SparkCutBlue900,

    success = SparkCutMint400,
    warning = SparkCutAmber400,
    error = SparkCutCoral400,
    info = SparkCutBlue300,

    successContainer = SparkCutMint900,
    warningContainer = SparkCutAmber900,
    errorContainer = SparkCutCoral900,
    infoContainer = Color(0xFF132432)
)

internal val SparkCutMaterialDarkColorScheme = darkColorScheme(
    primary = SparkCutDarkColorScheme.primary,
    onPrimary = SparkCutInk950,
    primaryContainer = SparkCutDarkColorScheme.primaryMuted,
    onPrimaryContainer = SparkCutIce50,

    secondary = SparkCutViolet400,
    onSecondary = SparkCutIce50,
    secondaryContainer = SparkCutViolet900,
    onSecondaryContainer = SparkCutIce50,

    tertiary = SparkCutMint400,
    onTertiary = SparkCutInk950,
    tertiaryContainer = SparkCutMint900,
    onTertiaryContainer = SparkCutIce50,

    background = SparkCutDarkColorScheme.background,
    onBackground = SparkCutDarkColorScheme.textPrimary,

    surface = SparkCutDarkColorScheme.surface,
    onSurface = SparkCutDarkColorScheme.textPrimary,
    surfaceVariant = SparkCutDarkColorScheme.surfaceElevated,
    onSurfaceVariant = SparkCutDarkColorScheme.textSecondary,

    error = SparkCutDarkColorScheme.error,
    onError = SparkCutIce50,
    errorContainer = SparkCutDarkColorScheme.errorContainer,
    onErrorContainer = SparkCutIce50,

    outline = SparkCutDarkColorScheme.strokeSoft,
    outlineVariant = SparkCutDarkColorScheme.strokeStrong,

    scrim = Color(0xCC000000),
    inverseSurface = SparkCutIce50,
    inverseOnSurface = SparkCutInk950,
    inversePrimary = SparkCutBlue300
)