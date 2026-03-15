package com.muratcangzm.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.muratcangzm.designsystem.theme.SparkCutTheme

@Composable
fun SparkCutScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    contentWindowInsets: WindowInsets = WindowInsets.safeDrawing,
    containerColor: Color = SparkCutTheme.colors.background,
    content: @Composable (PaddingValues) -> Unit
) {
    val resolvedSnackbarHostState = snackbarHostState ?: remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        snackbarHost = {
            SnackbarHost(hostState = resolvedSnackbarHostState)
        },
        containerColor = Color.Transparent,
        contentWindowInsets = contentWindowInsets
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(containerColor)
        ) {
            content(innerPadding)
        }
    }
}