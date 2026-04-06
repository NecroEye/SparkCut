package com.muratcangzm.sparkcut.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.muratcangzm.editor.ui.EditorContract
import com.muratcangzm.editor.ui.EditorScreen
import com.muratcangzm.export.ui.ExportContract
import com.muratcangzm.export.ui.ExportScreen
import com.muratcangzm.home.ui.HomeScreen
import com.muratcangzm.projects.ui.ProjectsScreen
import com.muratcangzm.settings.ui.SettingsScreen

@Composable
fun SparkCutNavHost(
    navigator: AppNavigator,
) {
    val entryProvider = remember(navigator) {
        entryProvider {
            entry<HomeRoute> {
                HomeScreen(
                    onOpenEditor = { mediaUris ->
                        navigator.goToEditor(
                            mediaUris = mediaUris,
                            projectId = null,
                        )
                    },
                    onOpenProject = { projectId ->
                        navigator.goToEditor(
                            mediaUris = emptyList(),
                            projectId = projectId,
                        )
                    },
                    onOpenSettings = {
                        navigator.goToSettings()
                    },
                )
            }

            entry<SettingsRoute> {
                SettingsScreen(
                    onBack = {
                        navigator.pop()
                    },
                )
            }

            entry<ProjectsRoute> {
                ProjectsScreen(
                    onOpenProject = { projectId ->
                        navigator.goToEditor(
                            mediaUris = emptyList(),
                            projectId = projectId,
                        )
                    },
                )
            }

            entry<EditorRoute> { key ->
                EditorScreen(
                    mediaUris = key.mediaUris,
                    projectId = key.projectId,
                    onBack = {
                        navigator.pop()
                    },
                    onOpenExport = { payload: EditorContract.ExportPayload ->
                        navigator.goToExport(
                            ExportRoute(
                                projectId = payload.projectId,
                                templateId = payload.templateId.value,
                                mediaClips = payload.mediaClips.map { clip ->
                                    ExportMediaClipNavArg(
                                        uri = clip.uri,
                                        trimStartMs = clip.trimStartMs,
                                        trimEndMs = clip.trimEndMs,
                                    )
                                },
                                textValues = payload.textValues.map { text ->
                                    ExportTextValueNavArg(
                                        fieldId = text.fieldId.value,
                                        value = text.value,
                                    )
                                },
                                transitionPresetName = payload.transition.name,
                                aspectRatioLabel = payload.aspectRatioLabel,
                                backgroundMusicUri = payload.backgroundMusicUri,
                            )
                        )
                    },
                )
            }

            entry<ExportRoute> { key ->
                ExportScreen(
                    launchArgs = ExportContract.LaunchArgs(
                        projectId = key.projectId,
                        templateId = key.templateId,
                        mediaClips = key.mediaClips.map { clip ->
                            ExportContract.MediaClipArg(
                                uri = clip.uri,
                                trimStartMs = clip.trimStartMs,
                                trimEndMs = clip.trimEndMs,
                            )
                        },
                        textValues = key.textValues.map { text ->
                            ExportContract.TextValueArg(
                                fieldId = text.fieldId,
                                value = text.value,
                            )
                        },
                        transitionPresetName = key.transitionPresetName,
                        aspectRatioLabel = key.aspectRatioLabel,
                        backgroundMusicUri = key.backgroundMusicUri,
                    ),
                    onBack = {
                        navigator.pop()
                    },
                )
            }
        }
    }

    NavDisplay(
        backStack = navigator.backStack,
        onBack = {
            navigator.pop()
        },
        entryProvider = entryProvider,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
    )
}
