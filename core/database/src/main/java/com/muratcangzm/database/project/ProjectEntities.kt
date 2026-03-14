package com.muratcangzm.database.project

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "projects",
)
data class ProjectEntity(
    @PrimaryKey
    @ColumnInfo(name = "project_id")
    val projectId: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "template_id")
    val templateId: String,
    @ColumnInfo(name = "aspect_ratio")
    val aspectRatio: String,

    @ColumnInfo(name = "audio_source_kind")
    val audioSourceKind: String,
    @ColumnInfo(name = "audio_track_id")
    val audioTrackId: String?,
    @ColumnInfo(name = "audio_local_uri")
    val audioLocalUri: String?,
    @ColumnInfo(name = "audio_start_ms")
    val audioStartMs: Long,
    @ColumnInfo(name = "audio_end_ms")
    val audioEndMs: Long?,
    @ColumnInfo(name = "audio_volume")
    val audioVolume: Float,

    @ColumnInfo(name = "cover_media_asset_id")
    val coverMediaAssetId: String?,

    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "project_slot_bindings",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["project_id"],
            childColumns = ["project_owner_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("project_owner_id")],
)
data class ProjectSlotBindingEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "row_id")
    val rowId: Long = 0L,
    @ColumnInfo(name = "project_owner_id")
    val projectOwnerId: String,
    @ColumnInfo(name = "slot_id")
    val slotId: String,
    @ColumnInfo(name = "media_asset_id")
    val mediaAssetId: String,
    @ColumnInfo(name = "order_index")
    val orderIndex: Int,
    @ColumnInfo(name = "trim_start_ms")
    val trimStartMs: Long?,
    @ColumnInfo(name = "trim_end_ms")
    val trimEndMs: Long?,
)

@Entity(
    tableName = "project_text_values",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["project_id"],
            childColumns = ["project_owner_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("project_owner_id")],
)
data class ProjectTextValueEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "row_id")
    val rowId: Long = 0L,
    @ColumnInfo(name = "project_owner_id")
    val projectOwnerId: String,
    @ColumnInfo(name = "field_id")
    val fieldId: String,
    @ColumnInfo(name = "value")
    val value: String,
)

@Entity(
    tableName = "project_transition_overrides",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["project_id"],
            childColumns = ["project_owner_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("project_owner_id")],
)
data class ProjectTransitionOverrideEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "row_id")
    val rowId: Long = 0L,
    @ColumnInfo(name = "project_owner_id")
    val projectOwnerId: String,
    @ColumnInfo(name = "slot_id")
    val slotId: String,
    @ColumnInfo(name = "transition")
    val transition: String,
)
