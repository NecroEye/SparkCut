package com.muratcangzm.media.domain

import com.muratcangzm.media.domain.model.MediaResolveBatchResult
import com.muratcangzm.model.media.MediaAsset

interface MediaAssetResolver {
    fun resolve(uri: String): MediaAsset?
    fun resolveAll(uris: List<String>): MediaResolveBatchResult
}