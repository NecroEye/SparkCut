package com.muratcangzm.media.data.export

import androidx.annotation.OptIn
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.DefaultGainProvider
import androidx.media3.common.audio.GainProcessor
import androidx.media3.common.util.UnstableApi
import com.muratcangzm.media.domain.export.AudioMixRequest

@OptIn(UnstableApi::class)
internal object Media3AudioProcessorFactory {

    fun createClipAudioProcessors(
        audioMix: AudioMixRequest,
    ): List<AudioProcessor> {
        if (!audioMix.preserveOriginalClipAudio) return emptyList()

        val gain = audioMix.clipAudioVolume.coerceIn(0f, 1f)
        if (gain >= 0.999f) return emptyList()

        val gainProvider = DefaultGainProvider.Builder(gain).build()
        return listOf(GainProcessor(gainProvider))
    }

    fun createBackgroundAudioProcessors(
        audioMix: AudioMixRequest,
    ): List<AudioProcessor> {
        if (audioMix.backgroundTrack == null) return emptyList()

        val gain = audioMix.backgroundAudioVolume.coerceIn(0f, 1f)
        if (gain >= 0.999f) return emptyList()

        val gainProvider = DefaultGainProvider.Builder(gain).build()
        return listOf(GainProcessor(gainProvider))
    }
}