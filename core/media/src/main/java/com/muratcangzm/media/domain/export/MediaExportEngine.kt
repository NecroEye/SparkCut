package com.muratcangzm.media.domain.export

import kotlinx.coroutines.flow.StateFlow

interface MediaExportEngine {
    fun createSession(request: MediaExportRequest): MediaExportSession
}

interface MediaExportSession {
    val state: StateFlow<MediaExportState>
    fun start()
    fun cancel()
}