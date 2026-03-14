package com.muratcangzm.model.media

import com.muratcangzm.model.id.MediaAssetId

enum class MediaType {
    IMAGE,
    VIDEO,
}

enum class MediaSourceType {
    LOCAL_GALLERY,
    LOCAL_CAMERA,
    LOCAL_EXPORT,
}

data class MediaAsset(
    val id: MediaAssetId,
    val uri: String,
    val type: MediaType,
    val mimeType: String? = null,
    val fileName: String? = null,
    val sizeBytes: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationMs: Long? = null,
    val rotationDegrees: Int = 0,
    val sourceType: MediaSourceType = MediaSourceType.LOCAL_GALLERY,
    val addedAtEpochMillis: Long? = null,
) {
    init {
        require(uri.isNotBlank()) { "MediaAsset uri cannot be blank." }
        require(rotationDegrees in setOf(0, 90, 180, 270)) {
            "rotationDegrees must be one of 0, 90, 180, 270."
        }
        require(sizeBytes == null || sizeBytes >= 0L) {
            "sizeBytes cannot be negative."
        }
        require(durationMs == null || durationMs >= 0L) {
            "durationMs cannot be negative."
        }
        require(width == null || width > 0) {
            "width must be greater than 0."
        }
        require(height == null || height > 0) {
            "height must be greater than 0."
        }
    }

    val isImage: Boolean
        get() = type == MediaType.IMAGE

    val isVideo: Boolean
        get() = type == MediaType.VIDEO

    val hasDimensions: Boolean
        get() = width != null && height != null
}