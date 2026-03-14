package com.muratcangzm.media.domain.export

interface AudioTrackMetadataReader {
    suspend fun read(uri: String): AudioTrackMetadata?
}
