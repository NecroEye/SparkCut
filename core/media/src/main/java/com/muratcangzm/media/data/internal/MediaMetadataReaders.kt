package com.muratcangzm.media.data.internal

import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns

internal data class CommonMetadata(
    val fileName: String?,
    val sizeBytes: Long?,
)

internal data class ImageMetadata(
    val width: Int?,
    val height: Int?,
    val rotationDegrees: Int,
)

internal data class VideoMetadata(
    val width: Int?,
    val height: Int?,
    val durationMs: Long?,
    val rotationDegrees: Int,
)

internal fun readCommonMetadata(
    contentResolver: ContentResolver,
    uri: Uri,
): CommonMetadata {
    val projection = arrayOf(
        OpenableColumns.DISPLAY_NAME,
        OpenableColumns.SIZE,
    )

    return runCatching {
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

            if (cursor.moveToFirst()) {
                val fileName = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                val sizeBytes = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    cursor.getLong(sizeIndex)
                } else {
                    null
                }

                CommonMetadata(
                    fileName = fileName,
                    sizeBytes = sizeBytes,
                )
            } else {
                CommonMetadata(
                    fileName = uri.lastPathSegment,
                    sizeBytes = null,
                )
            }
        } ?: CommonMetadata(
            fileName = uri.lastPathSegment,
            sizeBytes = null,
        )
    }.getOrElse {
        CommonMetadata(
            fileName = uri.lastPathSegment,
            sizeBytes = null,
        )
    }
}

internal fun readImageMetadata(
    context: Context,
    uri: Uri,
): ImageMetadata {
    val contentResolver = context.contentResolver

    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }

    runCatching {
        contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }
    }

    val rotationDegrees = runCatching {
        contentResolver.openInputStream(uri)?.use { input ->
            when (ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
    }.getOrDefault(0)

    return ImageMetadata(
        width = bounds.outWidth.takeIf { it > 0 },
        height = bounds.outHeight.takeIf { it > 0 },
        rotationDegrees = rotationDegrees,
    )
}

internal fun readVideoMetadata(
    context: Context,
    uri: Uri,
): VideoMetadata {
    val retriever = MediaMetadataRetriever()

    return runCatching {
        retriever.setDataSource(context, uri)

        val width = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH,
        )?.toIntOrNull()

        val height = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT,
        )?.toIntOrNull()

        val durationMs = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_DURATION,
        )?.toLongOrNull()

        val rotationDegrees = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION,
        )?.toIntOrNull()
            ?.let(::normalizeRotationDegrees)
            ?: 0

        VideoMetadata(
            width = width,
            height = height,
            durationMs = durationMs,
            rotationDegrees = rotationDegrees,
        )
    }.getOrElse {
        VideoMetadata(
            width = null,
            height = null,
            durationMs = null,
            rotationDegrees = 0,
        )
    }.also {
        runCatching { retriever.release() }
    }
}

internal fun normalizeRotationDegrees(value: Int): Int = when (value) {
    90, 180, 270 -> value
    else -> 0
}