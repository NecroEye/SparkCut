package com.muratcangzm.media.data.export

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.muratcangzm.media.domain.export.MediaExportFilePublisher
import com.muratcangzm.media.domain.export.PublishedMediaFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AndroidMediaStoreExportFilePublisher(
    private val context: Context,
) : MediaExportFilePublisher {

    override suspend fun publishVideo(
        inputFilePath: String,
        desiredDisplayName: String,
    ): PublishedMediaFile = withContext(Dispatchers.IO) {
        val sourceFile = File(inputFilePath)
        require(sourceFile.exists()) {
            "Exported file does not exist: $inputFilePath"
        }

        val safeDisplayName = desiredDisplayName
            .trim()
            .ifBlank { "SparkCut_Export" }
            .replace(Regex("[^a-zA-Z0-9._ -]"), "_")
            .let { name ->
                if (name.endsWith(".mp4", ignoreCase = true)) name else "$name.mp4"
            }

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, safeDisplayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(
                MediaStore.Video.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_MOVIES}/SparkCut"
            )
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val targetUri = requireNotNull(resolver.insert(collection, values)) {
            "Could not create MediaStore entry."
        }

        try {
            resolver.openOutputStream(targetUri)?.use { output ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: error("Could not open MediaStore output stream.")

            val finalizeValues = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            resolver.update(targetUri, finalizeValues, null, null)

            PublishedMediaFile(
                contentUri = targetUri.toString(),
                displayName = safeDisplayName,
                sizeBytes = sourceFile.length(),
            )
        } catch (t: Throwable) {
            resolver.delete(targetUri, null, null)
            throw t
        }
    }
}