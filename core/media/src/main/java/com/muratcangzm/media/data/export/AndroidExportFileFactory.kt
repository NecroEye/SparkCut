package com.muratcangzm.media.data.export

import android.content.Context
import com.muratcangzm.model.id.ExportJobId
import java.io.File

internal class AndroidExportFileFactory(
    private val context: Context,
) {

    fun createOutputFile(
        jobId: ExportJobId,
        baseName: String,
    ): File {
        val safeBaseName = baseName
            .trim()
            .ifBlank { "sparkcut_export" }
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")

        val exportDir = File(context.cacheDir, "exports").apply {
            mkdirs()
        }

        return File(
            exportDir,
            "${safeBaseName}_${jobId.value.take(8)}.mp4",
        )
    }
}