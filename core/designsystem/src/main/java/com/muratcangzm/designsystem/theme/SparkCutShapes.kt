package com.muratcangzm.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

@Immutable
data class SparkCutShapes(
    val xs: RoundedCornerShape,
    val sm: RoundedCornerShape,
    val md: RoundedCornerShape,
    val lg: RoundedCornerShape,
    val xl: RoundedCornerShape,
    val pill: RoundedCornerShape
)

internal val SparkCutDefaultShapes = SparkCutShapes(
    xs = RoundedCornerShape(10.dp),
    sm = RoundedCornerShape(12.dp),
    md = RoundedCornerShape(16.dp),
    lg = RoundedCornerShape(20.dp),
    xl = RoundedCornerShape(24.dp),
    pill = RoundedCornerShape(percent = 50)
)