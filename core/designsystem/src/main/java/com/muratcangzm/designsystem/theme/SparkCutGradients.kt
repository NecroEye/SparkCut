package com.muratcangzm.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush

@Immutable
data class SparkCutGradients(
    val primary: Brush,
    val accent: Brush,
    val success: Brush
)

internal val SparkCutDarkGradients = SparkCutGradients(
    primary = Brush.linearGradient(
        colors = listOf(
            SparkCutBlue400,
            SparkCutViolet400
        )
    ),
    accent = Brush.linearGradient(
        colors = listOf(
            SparkCutBlue300,
            SparkCutMint300
        )
    ),
    success = Brush.linearGradient(
        colors = listOf(
            SparkCutMint400,
            SparkCutMint300
        )
    )
)