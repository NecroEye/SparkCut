package com.muratcangzm.media.domain.export

data class PublishedMediaFile(
    val contentUri: String,
    val displayName: String,
    val sizeBytes: Long?,
)

interface MediaExportFilePublisher {
    suspend fun publishVideo(
        inputFilePath: String,
        desiredDisplayName: String,
    ): PublishedMediaFile
}