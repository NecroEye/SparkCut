package com.muratcangzm.media.data

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import android.util.Size
import com.muratcangzm.media.domain.MediaThumbnailProvider
import androidx.core.net.toUri

class AndroidMediaThumbnailProvider(
    private val context: Context,
) : MediaThumbnailProvider {

    private val memoryCache = object : LruCache<String, Bitmap>(20 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    override fun loadThumbnail(
        uri: String,
        sizePx: Int,
    ): Bitmap? {
        if (uri.isBlank()) return null
        if (sizePx <= 0) return null

        val cacheKey = "$uri#$sizePx"
        memoryCache.get(cacheKey)?.let { return it }

        val bitmap = runCatching {
            context.contentResolver.loadThumbnail(
                uri.toUri(),
                Size(sizePx, sizePx),
                null,
            )
        }.getOrNull()

        if (bitmap != null) {
            memoryCache.put(cacheKey, bitmap)
        }

        return bitmap
    }
}