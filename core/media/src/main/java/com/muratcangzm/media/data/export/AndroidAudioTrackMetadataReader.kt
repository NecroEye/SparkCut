package com.muratcangzm.media.data.export

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.muratcangzm.media.domain.export.AudioTrackMetadata
import com.muratcangzm.media.domain.export.AudioTrackMetadataReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

class AndroidAudioTrackMetadataReader(
    private val context: Context,
) : AudioTrackMetadataReader {

    override suspend fun read(uri: String): AudioTrackMetadata? = withContext(Dispatchers.IO) {
        runCatching {
            val parsedUri = uri.toUri()

            val mimeType = context.contentResolver.getType(parsedUri)

            var displayName: String? = null
            var sizeBytes: Long? = null

            context.contentResolver.query(
                parsedUri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                    if (nameIndex >= 0) {
                        displayName = cursor.getString(nameIndex)
                    }

                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }

            val durationMs = MediaMetadataRetriever().useForContextUri(context, parsedUri) {
                extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            }

            AudioTrackMetadata(
                uri = uri,
                displayName = displayName ?: parsedUri.lastPathSegment ?: "Selected soundtrack",
                mimeType = mimeType,
                sizeBytes = sizeBytes,
                durationMs = durationMs,
            )
        }.getOrNull()
    }
}

private inline fun <T> MediaMetadataRetriever.useForContextUri(
    context: Context,
    uri: Uri,
    block: MediaMetadataRetriever.() -> T,
): T {
    return try {
        setDataSource(context, uri)
        block()
    } finally {
        release()
    }
}
