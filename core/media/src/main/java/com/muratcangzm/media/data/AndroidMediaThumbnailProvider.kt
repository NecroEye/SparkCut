package com.muratcangzm.media.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.LruCache
import android.util.Size
import androidx.core.net.toUri
import com.muratcangzm.media.domain.MediaThumbnailProvider

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

        val safeSize = sizePx.coerceIn(96, 1024)
        val cacheKey = "$uri#$safeSize"

        memoryCache.get(cacheKey)?.let { return it }

        val parsedUri = runCatching { uri.toUri() }.getOrNull() ?: return null
        val resolver = context.contentResolver
        val mimeType = resolver.getType(parsedUri).orEmpty().lowercase()

        val bitmap = when {
            mimeType.startsWith("video/") -> {
                loadPlatformThumbnail(
                    uri = parsedUri,
                    sizePx = safeSize,
                )
            }

            mimeType.startsWith("image/") -> {
                loadPlatformThumbnail(
                    uri = parsedUri,
                    sizePx = safeSize,
                ) ?: loadImageThumbnailSafely(
                    uri = parsedUri,
                    sizePx = safeSize,
                )
            }

            else -> {
                loadPlatformThumbnail(
                    uri = parsedUri,
                    sizePx = safeSize,
                ) ?: loadImageThumbnailSafely(
                    uri = parsedUri,
                    sizePx = safeSize,
                )
            }
        }

        if (bitmap != null) {
            memoryCache.put(cacheKey, bitmap)
        }

        return bitmap
    }

    private fun loadPlatformThumbnail(
        uri: Uri,
        sizePx: Int,
    ): Bitmap? {
        return runCatching {
            context.contentResolver.loadThumbnail(
                uri,
                Size(sizePx, sizePx),
                null,
            )
        }.getOrNull()
    }

    private fun loadImageThumbnailSafely(
        uri: Uri,
        sizePx: Int,
    ): Bitmap? {
        return runCatching {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false

                val width = info.size.width.coerceAtLeast(1)
                val height = info.size.height.coerceAtLeast(1)
                val maxSide = maxOf(width, height)
                val sampleSize = (maxSide / sizePx).coerceAtLeast(1)

                decoder.setTargetSampleSize(sampleSize)
            }
        }.getOrNull()
    }
}