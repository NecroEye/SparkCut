package com.muratcangzm.media.domain.export

data class BackgroundAudioTrackRequest(
    val uri: String,
    val loop: Boolean = true,
)

data class AudioMixRequest(
    val preserveOriginalClipAudio: Boolean,
    val backgroundTrack: BackgroundAudioTrackRequest? = null,
    val clipAudioVolume: Float = 1f,
    val backgroundAudioVolume: Float = 1f,
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L,
)