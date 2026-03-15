package com.muratcangzm.sparkcut.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.muratcangzm.designsystem.theme.SparkCutTheme
import com.muratcangzm.sparkcut.navigation.HomeRoute
import com.muratcangzm.sparkcut.navigation.SparkCutNavHost
import com.muratcangzm.sparkcut.navigation.rememberAppNavigator

@Composable
fun SparkCutApp() {
    SparkCutTheme {
        SparkCutSystemBarsStyle()

        val navigator = rememberAppNavigator(
            startRoute = HomeRoute
        )

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = SparkCutTheme.colors.background
        ) {
            SparkCutNavHost(
                navigator = navigator
            )
        }
    }
}