package com.muratcangzm.data.project

import com.muratcangzm.database.project.ProjectDao
import com.muratcangzm.database.project.ProjectDraftLocal
import com.muratcangzm.database.project.ProjectEntity
import com.muratcangzm.database.project.ProjectListItemLocal
import com.muratcangzm.database.project.ProjectMediaAssetEntity
import com.muratcangzm.database.project.ProjectSlotBindingEntity
import com.muratcangzm.database.project.ProjectTextValueEntity
import com.muratcangzm.database.project.ProjectTransitionOverrideEntity
import com.muratcangzm.model.id.AudioTrackId
import com.muratcangzm.model.id.MediaAssetId
import com.muratcangzm.model.id.ProjectId
import com.muratcangzm.model.id.SlotId
import com.muratcangzm.model.id.TemplateId
import com.muratcangzm.model.id.TextFieldId
import com.muratcangzm.model.project.AudioSourceKind
import com.muratcangzm.model.project.ProjectAudioSelection
import com.muratcangzm.model.project.ProjectDraft
import com.muratcangzm.model.project.ProjectEditorSession
import com.muratcangzm.model.project.ProjectListItem
import com.muratcangzm.model.project.ProjectMediaAssetRef
import com.muratcangzm.model.project.ProjectSlotBinding
import com.muratcangzm.model.project.ProjectStatus
import com.muratcangzm.model.project.ProjectTextValue
import com.muratcangzm.model.project.ProjectTransitionOverride
import com.muratcangzm.model.template.AspectRatio
import com.muratcangzm.model.template.TransitionPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomProjectDraftRepository(
    private val projectDao: ProjectDao,
) : ProjectDraftRepository {

    override fun observeProjectListItems(): Flow<List<ProjectListItem>> {
        return projectDao.observeProjectListItems()
            .map { items -> items.map(ProjectListItemLocal::toDomain) }
    }

    override fun observeProjectSession(projectId: ProjectId): Flow<ProjectEditorSession?> {
        return projectDao.observeProjectDraft(projectId.value)
            .map { it?.toDomain() }
    }

    override suspend fun getProjectSession(projectId: ProjectId): ProjectEditorSession? {
        return projectDao.getProjectDraft(projectId.value)?.toDomain()
    }

    override suspend fun saveSession(session: ProjectEditorSession) {
        projectDao.replaceProjectGraph(
            project = session.draft.toEntity(),
            slotBindings = session.draft.slotBindings.map { it.toEntity(session.draft.id) },
            textValues = session.draft.textValues.map { it.toEntity(session.draft.id) },
            transitionOverrides = session.draft.transitionOverrides.map { it.toEntity(session.draft.id) },
            mediaAssets = session.mediaAssets.map { it.toEntity(session.draft.id) },
        )
    }

    override suspend fun updateStatus(
        projectId: ProjectId,
        status: ProjectStatus,
        updatedAtEpochMillis: Long,
    ) {
        projectDao.updateProjectStatus(
            projectId = projectId.value,
            status = status.name,
            updatedAtEpochMillis = updatedAtEpochMillis,
        )
    }

    override suspend fun deleteProject(projectId: ProjectId) {
        projectDao.deleteProject(projectId.value)
    }
}

private fun ProjectDraft.toEntity(): ProjectEntity =
    ProjectEntity(
        projectId = id.value,
        name = name,
        templateId = templateId.value,
        aspectRatio = aspectRatio.name,
        audioSourceKind = audioSelection.sourceKind.name,
        audioTrackId = audioSelection.audioTrackId?.value,
        audioLocalUri = audioSelection.localUri,
        audioStartMs = audioSelection.startMs,
        audioEndMs = audioSelection.endMs,
        audioVolume = audioSelection.volume,
        coverMediaAssetId = coverMediaAssetId?.value,
        status = status.name,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )

private fun ProjectSlotBinding.toEntity(projectId: ProjectId): ProjectSlotBindingEntity =
    ProjectSlotBindingEntity(
        projectOwnerId = projectId.value,
        slotId = slotId.value,
        mediaAssetId = mediaAssetId.value,
        orderIndex = order,
        trimStartMs = trimStartMs,
        trimEndMs = trimEndMs,
    )

private fun ProjectTextValue.toEntity(projectId: ProjectId): ProjectTextValueEntity =
    ProjectTextValueEntity(
        projectOwnerId = projectId.value,
        fieldId = fieldId.value,
        value = value,
    )

private fun ProjectTransitionOverride.toEntity(projectId: ProjectId): ProjectTransitionOverrideEntity =
    ProjectTransitionOverrideEntity(
        projectOwnerId = projectId.value,
        slotId = slotId.value,
        transition = transition.name,
    )

private fun ProjectMediaAssetRef.toEntity(projectId: ProjectId): ProjectMediaAssetEntity =
    ProjectMediaAssetEntity(
        projectOwnerId = projectId.value,
        mediaAssetId = id.value,
        sourceUri = sourceUri,
        fileName = fileName,
        mimeType = mimeType,
        width = width,
        height = height,
        durationMs = durationMs,
    )

private fun ProjectDraftLocal.toDomain(): ProjectEditorSession =
    ProjectEditorSession(
        draft = ProjectDraft(
            id = ProjectId(project.projectId),
            name = project.name,
            templateId = TemplateId(project.templateId),
            aspectRatio = enumValueOf<AspectRatio>(project.aspectRatio),
            slotBindings = slotBindings
                .sortedBy { it.orderIndex }
                .map {
                    ProjectSlotBinding(
                        slotId = SlotId(it.slotId),
                        mediaAssetId = MediaAssetId(it.mediaAssetId),
                        order = it.orderIndex,
                        trimStartMs = it.trimStartMs,
                        trimEndMs = it.trimEndMs,
                    )
                },
            textValues = textValues.map {
                ProjectTextValue(
                    fieldId = TextFieldId(it.fieldId),
                    value = it.value,
                )
            },
            transitionOverrides = transitionOverrides.map {
                ProjectTransitionOverride(
                    slotId = SlotId(it.slotId),
                    transition = enumValueOf<TransitionPreset>(it.transition),
                )
            },
            audioSelection = ProjectAudioSelection(
                sourceKind = enumValueOf<AudioSourceKind>(project.audioSourceKind),
                audioTrackId = project.audioTrackId?.let(::AudioTrackId),
                localUri = project.audioLocalUri,
                startMs = project.audioStartMs,
                endMs = project.audioEndMs,
                volume = project.audioVolume,
            ),
            coverMediaAssetId = project.coverMediaAssetId?.let(::MediaAssetId),
            status = enumValueOf<ProjectStatus>(project.status),
            createdAtEpochMillis = project.createdAtEpochMillis,
            updatedAtEpochMillis = project.updatedAtEpochMillis,
        ),
        mediaAssets = mediaAssets.map {
            ProjectMediaAssetRef(
                id = MediaAssetId(it.mediaAssetId),
                sourceUri = it.sourceUri,
                fileName = it.fileName,
                mimeType = it.mimeType,
                width = it.width,
                height = it.height,
                durationMs = it.durationMs,
            )
        },
    )

private fun ProjectListItemLocal.toDomain(): ProjectListItem =
    ProjectListItem(
        id = ProjectId(projectId),
        name = name,
        templateId = TemplateId(templateId),
        aspectRatio = enumValueOf<AspectRatio>(aspectRatio),
        mediaCount = mediaCount,
        updatedAtEpochMillis = updatedAtEpochMillis,
        status = enumValueOf<ProjectStatus>(status),
    )
