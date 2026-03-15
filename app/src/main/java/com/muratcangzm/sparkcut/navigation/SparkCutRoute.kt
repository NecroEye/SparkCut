package com.muratcangzm.sparkcut.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : NavKey

@Serializable
data object ProjectsRoute : NavKey

@Serializable
data object TemplatesRoute : NavKey

@Serializable
data class CreateRoute(
    val templateId: String,
) : NavKey

@Serializable
data class EditorRoute(
    val templateId: String? = null,
    val mediaUris: List<String> = emptyList(),
    val projectId: String? = null,
) : NavKey

@Serializable
data class ExportMediaClipNavArg(
    val uri: String,
    val trimStartMs: Long?,
    val trimEndMs: Long?,
)

@Serializable
data class ExportTextValueNavArg(
    val fieldId: String,
    val value: String,
)

@Serializable
data class ExportRoute(
    val projectId: String? = null,
    val templateId: String,
    val mediaClips: List<ExportMediaClipNavArg>,
    val textValues: List<ExportTextValueNavArg>,
    val transitionPresetName: String,
    val aspectRatioLabel: String,
    val backgroundMusicUri: String? = null,
) : NavKey
