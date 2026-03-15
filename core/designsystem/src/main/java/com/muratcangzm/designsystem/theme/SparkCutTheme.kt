package com.muratcangzm.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalSparkCutColors = staticCompositionLocalOf<SparkCutColorScheme> {
    error("SparkCutColorScheme was not provided")
}

private val LocalSparkCutGradients = staticCompositionLocalOf<SparkCutGradients> {
    error("SparkCutGradients was not provided")
}

private val LocalSparkCutSpacing = staticCompositionLocalOf<SparkCutSpacing> {
    error("SparkCutSpacing was not provided")
}

private val LocalSparkCutShapes = staticCompositionLocalOf<SparkCutShapes> {
    error("SparkCutShapes was not provided")
}

private val LocalSparkCutTypography = staticCompositionLocalOf<SparkCutTypography> {
    error("SparkCutTypography was not provided")
}

object SparkCutTheme {

    val colors: SparkCutColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalSparkCutColors.current

    val gradients: SparkCutGradients
        @Composable
        @ReadOnlyComposable
        get() = LocalSparkCutGradients.current

    val spacing: SparkCutSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSparkCutSpacing.current

    val shapes: SparkCutShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalSparkCutShapes.current

    val typography: SparkCutTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalSparkCutTypography.current
}

@Composable
fun SparkCutTheme(
    content: @Composable () -> Unit
) {
   CompositionLocalProvider(
        LocalSparkCutColors provides SparkCutDarkColorScheme,
        LocalSparkCutGradients provides SparkCutDarkGradients,
        LocalSparkCutSpacing provides SparkCutDefaultSpacing,
        LocalSparkCutShapes provides SparkCutDefaultShapes,
        LocalSparkCutTypography provides SparkCutDefaultTypography
    ) {
        MaterialTheme(
            colorScheme = SparkCutMaterialDarkColorScheme,
            typography = SparkCutMaterialTypography,
            shapes = Shapes(
                small = SparkCutDefaultShapes.sm,
                medium = SparkCutDefaultShapes.md,
                large = SparkCutDefaultShapes.lg
            ),
            content = content
        )
    }
}