package com.muratcangzm.designsystem.theme

import androidx.compose.material3.Typography as MaterialTypography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class SparkCutTypography(
    val display: TextStyle,
    val screenTitle: TextStyle,
    val sectionTitle: TextStyle,
    val cardTitle: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val meta: TextStyle,
    val label: TextStyle,
    val button: TextStyle
)

private val SparkCutBaseFontFamily = FontFamily.Default

internal val SparkCutDefaultTypography = SparkCutTypography(
    display = TextStyle(
        fontFamily = SparkCutBaseFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.4).sp
    ),
    screenTitle = TextStyle(
        fontFamily = SparkCutBaseFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.2).sp
    ),
    sectionTitle = TextStyle(
        fontFamily = SparkCutBaseFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.1).sp
    ),
    cardTitle = TextStyle(
        fontFamily = SparkCutBaseFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    body = TextStyle(
        fontFamily = SparkCutBaseFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    bodyStrong = TextStyle(
        fontFamily = SparkCutBaseFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    meta = TextStyle(
        fontFamily = SparkCutBaseFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    label = TextStyle(
        fontFamily = SparkCutBaseFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    button = TextStyle(
        fontFamily = SparkCutBaseFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
)

internal val SparkCutMaterialTypography = MaterialTypography(
    displayLarge = SparkCutDefaultTypography.display,
    headlineMedium = SparkCutDefaultTypography.screenTitle,
    titleLarge = SparkCutDefaultTypography.sectionTitle,
    titleMedium = SparkCutDefaultTypography.cardTitle,
    bodyLarge = SparkCutDefaultTypography.body,
    bodyMedium = SparkCutDefaultTypography.body,
    labelLarge = SparkCutDefaultTypography.button,
    labelMedium = SparkCutDefaultTypography.label,
    bodySmall = SparkCutDefaultTypography.meta
)