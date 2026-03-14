package com.muratcangzm.media.data.export

import android.content.Context
import android.media.MediaMetadataRetriever
import com.muratcangzm.media.domain.export.AudioTrackDurationReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

class AndroidAudioTrackDurationReader(
    private val context: Context,
) : AudioTrackDurationReader {

    override suspend fun readDurationMs(uri: String): Long? = withContext(Dispatchers.IO) {
        runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri.toUri())
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()
            } finally {
                retriever.release()
            }
        }.getOrNull()
    }
}
