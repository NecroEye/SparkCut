package com.muratcangzm.media.domain.export

data class AudioTrackMetadata(
    val uri: String,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long?,
    val durationMs: Long?,
)
