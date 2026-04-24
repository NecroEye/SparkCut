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
                                isMuted = payload.isMuted,
                                audioTracks = payload.audioTracks.map { track ->
                                    ExportAudioTrackNavArg(
                                        uri = track.uri,
                                        fileName = track.fileName,
                                        durationMs = track.durationMs,
                                        trimStartMs = track.trimStartMs,
                                        trimEndMs = track.trimEndMs,
                                        volume = track.volume,
                                        offsetMs = track.offsetMs,
                                    )
                                },
                                resolutionWidth = payload.resolutionWidth,
                                resolutionHeight = payload.resolutionHeight,
                                captions = payload.captions.map { caption ->
                                    ExportCaptionNavArg(
                                        id = caption.id,
                                        text = caption.text,
                                        startMs = caption.startMs,
                                        endMs = caption.endMs,
                                    )
                                },
                                textOverlays = payload.textOverlays.map { overlay ->
                                    ExportTextOverlayNavArg(
                                        id = overlay.id,
                                        text = overlay.text,
                                        startMs = overlay.startMs,
                                        endMs = overlay.endMs,
                                        gravity = overlay.gravity,
                                        textSizeSp = overlay.textSizeSp,
                                    )
                                },
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
                        isMuted = key.isMuted,
                        audioTracks = key.audioTracks.map { track ->
                            ExportContract.AudioTrackArg(
                                uri = track.uri,
                                fileName = track.fileName,
                                durationMs = track.durationMs,
                                trimStartMs = track.trimStartMs,
                                trimEndMs = track.trimEndMs,
                                volume = track.volume,
                                offsetMs = track.offsetMs,
                            )
                        },
                        resolutionWidth = key.resolutionWidth,
                        resolutionHeight = key.resolutionHeight,
                        captions = key.captions.map { caption ->
                            ExportContract.CaptionArg(
                                id = caption.id,
                                text = caption.text,
                                startMs = caption.startMs,
                                endMs = caption.endMs,
                            )
                        },
                        textOverlays = key.textOverlays.map { overlay ->
                            ExportContract.TextOverlayArg(
                                id = overlay.id,
                                text = overlay.text,
                                startMs = overlay.startMs,
                                endMs = overlay.endMs,
                                gravity = overlay.gravity,
                                textSizeSp = overlay.textSizeSp,
                            )
                        },
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
