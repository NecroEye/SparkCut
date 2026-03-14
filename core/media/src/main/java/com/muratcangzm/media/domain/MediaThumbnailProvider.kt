package com.muratcangzm.media.domain

import android.graphics.Bitmap

interface MediaThumbnailProvider {
    fun loadThumbnail(
        uri: String,
        sizePx: Int,
    ): Bitmap?
}