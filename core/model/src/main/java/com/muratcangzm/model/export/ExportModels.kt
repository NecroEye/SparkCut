package com.muratcangzm.model.export

import com.muratcangzm.model.id.ExportJobId
import com.muratcangzm.model.id.ProjectId

enum class ExportResolution(val width: Int, val height: Int) {
    HD_720(720, 1280),
    FHD_1080(1080, 1920),
    QHD_2K(1440, 2560),
    UHD_4K(2160, 3840),
}

enum class ExportFps(val value: Int) {
    FPS_24(24),
    FPS_30(30),
    FPS_60(60),
}

enum class VideoCodec {
    H264,
    H265,
}

enum class AudioCodec {
    AAC,
}

data class ExportPreset(
    val id: String,
    val label: String,
    val resolution: ExportResolution,
    val fps: ExportFps,
    val videoCodec: VideoCodec = VideoCodec.H264,
    val audioCodec: AudioCodec = AudioCodec.AAC,
    val videoBitrateMbps: Int,
    val audioBitrateKbps: Int = 192,
    val includeWatermark: Boolean = false,
) {
    init {
        require(id.isNotBlank()) { "ExportPreset id cannot be blank." }
        require(label.isNotBlank()) { "ExportPreset label cannot be blank." }
        require(videoBitrateMbps > 0) { "videoBitrateMbps must be > 0." }
        require(audioBitrateKbps > 0) { "audioBitrateKbps must be > 0." }
    }
}

enum class ExportStatus {
    IDLE,
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELED,
}

data class ExportJob(
    val id: ExportJobId,
    val projectId: ProjectId,
    val preset: ExportPreset,
    val progress: Float = 0f,
    val status: ExportStatus = ExportStatus.IDLE,
    val outputUri: String? = null,
    val errorMessage: String? = null,
) {
    init {
        require(progress in 0f..1f) { "progress must be between 0f and 1f." }
    }

    val progressPercent: Int
        get() = (progress * 100).toInt()
}