package com.muratcangzm.database.project

data class ProjectListItemLocal(
    val projectId: String,
    val name: String,
    val templateId: String,
    val aspectRatio: String,
    val mediaCount: Int,
    val updatedAtEpochMillis: Long,
    val status: String,
)
