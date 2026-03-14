package com.muratcangzm.media.domain.export

interface AudioTrackDurationReader {
    suspend fun readDurationMs(uri: String): Long?
}