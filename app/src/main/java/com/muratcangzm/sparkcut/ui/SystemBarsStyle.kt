package com.muratcangzm.sparkcut.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.muratcangzm.designsystem.theme.SparkCutTheme

@Composable
internal fun SparkCutSystemBarsStyle() {
    val view = LocalView.current
    val context = LocalContext.current
    val colors = SparkCutTheme.colors

    if (view.isInEditMode) return

    SideEffect {
        val activity = context.findActivity() ?: return@SideEffect
        val window = activity.window

        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = colors.background.toArgb()

        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}