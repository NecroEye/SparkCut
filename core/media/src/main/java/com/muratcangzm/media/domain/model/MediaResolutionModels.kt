package com.muratcangzm.media.domain.model

import com.muratcangzm.model.media.MediaAsset

data class MediaResolveFailure(
    val uri: String,
    val reason: String,
)

data class MediaResolveBatchResult(
    val assets: List<MediaAsset>,
    val failures: List<MediaResolveFailure>,
) {
    val hasFailures: Boolean
        get() = failures.isNotEmpty()
}