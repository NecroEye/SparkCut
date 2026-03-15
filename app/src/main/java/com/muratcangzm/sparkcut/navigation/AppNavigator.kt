package com.muratcangzm.sparkcut.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

@Stable
class AppNavigator(
    val backStack: NavBackStack<NavKey>,
) {
    fun navigate(route: NavKey) {
        backStack.add(route)
    }

    fun replaceTop(route: NavKey) {
        if (backStack.isNotEmpty()) {
            backStack.removeAt(backStack.lastIndex)
        }
        backStack.add(route)
    }

    fun pop(): Boolean {
        return if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
            true
        } else {
            false
        }
    }

    fun popToRoot() {
        while (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    fun goToProjects() {
        navigate(ProjectsRoute)
    }

    fun goToTemplates() {
        navigate(TemplatesRoute)
    }

    fun goToCreate(templateId: String) {
        navigate(CreateRoute(templateId = templateId))
    }

    fun goToEditor(
        templateId: String?,
        mediaUris: List<String>,
        projectId: String?,
    ) {
        navigate(
            EditorRoute(
                templateId = templateId,
                mediaUris = mediaUris,
                projectId = projectId,
            )
        )
    }

    fun goToExport(route: ExportRoute) {
        navigate(route)
    }
}

@Composable
fun rememberAppNavigator(
    startRoute: NavKey = HomeRoute,
): AppNavigator {
    val backStack = rememberNavBackStack(startRoute)
    return remember(backStack) {
        AppNavigator(backStack = backStack)
    }
}
