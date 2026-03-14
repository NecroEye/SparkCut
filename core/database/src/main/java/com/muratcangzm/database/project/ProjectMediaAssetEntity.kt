package com.muratcangzm.database.project

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "project_media_assets",
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
data class ProjectMediaAssetEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "row_id")
    val rowId: Long = 0L,
    @ColumnInfo(name = "project_owner_id")
    val projectOwnerId: String,
    @ColumnInfo(name = "media_asset_id")
    val mediaAssetId: String,
    @ColumnInfo(name = "source_uri")
    val sourceUri: String,
    @ColumnInfo(name = "file_name")
    val fileName: String?,
    @ColumnInfo(name = "mime_type")
    val mimeType: String?,
    @ColumnInfo(name = "width")
    val width: Int?,
    @ColumnInfo(name = "height")
    val height: Int?,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long?,
)
