package com.muratcangzm.media.data.export

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.DefaultGainProvider
import androidx.media3.common.audio.GainProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.muratcangzm.media.domain.export.AudioMixRequest
import com.muratcangzm.media.domain.export.AudioTrackDurationReader
import com.muratcangzm.media.domain.export.ExportTextOverlay
import com.muratcangzm.media.domain.export.MediaExportEngine
import com.muratcangzm.media.domain.export.MediaExportRequest
import com.muratcangzm.media.domain.export.MediaExportSequenceItem
import com.muratcangzm.media.domain.export.MediaExportSession
import com.muratcangzm.media.domain.export.MediaExportState
import com.muratcangzm.media.domain.export.MediaTransitionPhase
import com.muratcangzm.media.domain.export.MediaTransitionWindow
import com.muratcangzm.model.export.AudioCodec
import com.muratcangzm.model.export.ExportFps
import com.muratcangzm.model.export.VideoCodec
import com.muratcangzm.model.media.MediaType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import java.io.File

class AndroidMedia3ExportEngine(
    private val context: Context,
    private val audioTrackDurationReader: AudioTrackDurationReader,
) : MediaExportEngine {

    override fun createSession(request: MediaExportRequest): MediaExportSession {
        return AndroidMedia3ExportSession(
            context = context,
            audioTrackDurationReader = audioTrackDurationReader,
            request = request,
        )
    }
}

private class AndroidMedia3ExportSession(
    private val context: Context,
    private val audioTrackDurationReader: AudioTrackDurationReader,
    private val request: MediaExportRequest,
) : MediaExportSession {

    private val outputFileFactory = AndroidExportFileFactory(context)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow<MediaExportState>(MediaExportState.Idle)
    override val state: StateFlow<MediaExportState> = _state

    @SuppressLint("UnsafeOptInUsageError")
    private var transformer: Transformer? = null
    private var outputFile: File? = null
    private var hasStarted = false
    private var isTerminal = false

    private val progressRunnable = object : Runnable {
        @OptIn(UnstableApi::class)
        override fun run() {
            val activeTransformer = transformer ?: return
            if (isTerminal) return

            val progressHolder = ProgressHolder()
            val progressState = activeTransformer.getProgress(progressHolder)

            if (progressState == Transformer.PROGRESS_STATE_AVAILABLE) {
                val progress = (progressHolder.progress / 100f).coerceIn(0f, 1f)
                _state.value = MediaExportState.Running(
                    progress = progress,
                    statusText = progress.toStatusText(),
                    outputFilePath = outputFile?.absolutePath.orEmpty(),
                )
            }

            if (progressState != Transformer.PROGRESS_STATE_NOT_STARTED && !isTerminal) {
                mainHandler.postDelayed(this, 250L)
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun start() {
        if (hasStarted) return
        hasStarted = true

        mainHandler.post {
            runCatching {
                require(request.sequenceItems.isNotEmpty()) {
                    "No export sequence items were provided."
                }

                val composition = runBlocking {
                    buildComposition(
                        sequenceItems = request.sequenceItems,
                    )
                }

                outputFile = outputFileFactory.createOutputFile(
                    jobId = request.jobId,
                    baseName = request.outputFileBaseName,
                ).also { file ->
                    if (file.exists()) {
                        file.delete()
                    }
                }

                transformer = Transformer.Builder(context)
                    .setVideoMimeType(request.preset.videoCodec.toVideoMimeType())
                    .setAudioMimeType(request.preset.audioCodec.toAudioMimeType())
                    .addListener(
                        object : Transformer.Listener {
                            override fun onCompleted(
                                composition: Composition,
                                exportResult: ExportResult,
                            ) {
                                isTerminal = true
                                mainHandler.removeCallbacks(progressRunnable)
                                _state.value = MediaExportState.Completed(
                                    outputFilePath = outputFile?.absolutePath.orEmpty(),
                                    fileSizeBytes = outputFile?.takeIf(File::exists)?.length(),
                                )
                            }

                            override fun onError(
                                composition: Composition,
                                exportResult: ExportResult,
                                exportException: ExportException,
                            ) {
                                isTerminal = true
                                mainHandler.removeCallbacks(progressRunnable)
                                _state.value = MediaExportState.Failed(
                                    message = exportException.message ?: "Unknown export error.",
                                    outputFilePath = outputFile?.absolutePath,
                                )
                            }
                        },
                    )
                    .build()

                _state.value = MediaExportState.Running(
                    progress = 0f,
                    statusText = "Preparing export…",
                    outputFilePath = outputFile!!.absolutePath,
                )

                transformer!!.start(composition, outputFile!!.absolutePath)
                mainHandler.post(progressRunnable)
            }.onFailure { throwable ->
                isTerminal = true
                mainHandler.removeCallbacks(progressRunnable)
                _state.value = MediaExportState.Failed(
                    message = throwable.message ?: "Export failed before it could start.",
                    outputFilePath = outputFile?.absolutePath,
                )
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun cancel() {
        mainHandler.post {
            if (isTerminal) return@post
            isTerminal = true
            transformer?.cancel()
            mainHandler.removeCallbacks(progressRunnable)
            _state.value = MediaExportState.Cancelled(
                outputFilePath = outputFile?.absolutePath,
            )
        }
    }

    @OptIn(UnstableApi::class)
    private suspend fun buildComposition(
        sequenceItems: List<MediaExportSequenceItem>,
    ): Composition {
        val segmentedItems = segmentSequenceForEffects(
            sequenceItems = sequenceItems,
            overlays = request.textOverlays,
            transitions = request.transitionWindows,
        )

        val hasAnyVideoClipWithAudio =
            request.audioMix.preserveOriginalClipAudio &&
                    segmentedItems.any { it.mediaType == MediaType.VIDEO }

        val primaryTrackTypes = buildSet {
            add(C.TRACK_TYPE_VIDEO)
            if (hasAnyVideoClipWithAudio) {
                add(C.TRACK_TYPE_AUDIO)
            }
        }

        val videoSequenceBuilder = EditedMediaItemSequence.Builder(primaryTrackTypes)

        segmentedItems.forEach { segment ->
            val mediaItem = when (segment.mediaType) {
                MediaType.IMAGE -> {
                    MediaItem.Builder()
                        .setUri(segment.uri)
                        .setImageDurationMs(segment.durationMs)
                        .build()
                }

                MediaType.VIDEO -> {
                    val clippingBuilder = MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(segment.trimStartMs ?: 0L)

                    segment.trimEndMs?.let(clippingBuilder::setEndPositionMs)

                    MediaItem.Builder()
                        .setUri(segment.uri)
                        .setClippingConfiguration(clippingBuilder.build())
                        .build()
                }
            }

            val videoEffects = mutableListOf<Effect>().apply {
                addAll(Media3VisualStyleMapper.map(request.visualStyle))
                addAll(Media3TransitionEffectMapper.map(segment.activeTransition))
                Media3OverlayEffectFactory
                    .createTextOverlayEffect(segment.activeOverlays)
                    ?.let(::add)
            }

            val clipAudioProcessors = if (segment.mediaType == MediaType.VIDEO) {
                Media3AudioProcessorFactory.createClipAudioProcessors(request.audioMix)
            } else {
                emptyList()
            }

            val editedMediaItemBuilder = EditedMediaItem.Builder(mediaItem)
                .setEffects(
                    Effects(
                        clipAudioProcessors,
                        videoEffects,
                    ),
                )

            if (segment.mediaType == MediaType.IMAGE) {
                editedMediaItemBuilder.setFrameRate(request.preset.fps.toFrameRate())
            }

            if (segment.mediaType == MediaType.VIDEO && !request.audioMix.preserveOriginalClipAudio) {
                editedMediaItemBuilder.setRemoveAudio(true)
            }

            val editedMediaItem = editedMediaItemBuilder.build()
            videoSequenceBuilder.addItem(editedMediaItem)
        }

        val videoSequence = videoSequenceBuilder.build()

        val backgroundTrack = request.audioMix.backgroundTrack
            ?: return Composition.Builder(videoSequence).build()

        val totalDurationMs = sequenceItems.sumOf { it.durationMs }

        val soundtrackDurationMs = audioTrackDurationReader.readDurationMs(backgroundTrack.uri)
        require(soundtrackDurationMs != null && soundtrackDurationMs > 0L) {
            "Selected soundtrack could not be read or has invalid duration."
        }

        val backgroundSegments = buildBackgroundAudioSegments(
            backgroundUri = backgroundTrack.uri,
            totalDurationMs = totalDurationMs,
            sourceDurationMs = soundtrackDurationMs,
            shouldLoop = backgroundTrack.loop,
            audioMix = request.audioMix,
            overlays = request.textOverlays,
        )

        if (backgroundSegments.isEmpty()) {
            return Composition.Builder(videoSequence).build()
        }

        val backgroundAudioSequenceBuilder = EditedMediaItemSequence.Builder(
            setOf(C.TRACK_TYPE_AUDIO),
        )

        backgroundSegments.forEach { segment ->
            val clippingConfiguration = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(segment.startMs)
                .setEndPositionMs(segment.endMs)
                .build()

            val backgroundAudioItem = EditedMediaItem.Builder(
                MediaItem.Builder()
                    .setUri(segment.uri)
                    .setClippingConfiguration(clippingConfiguration)
                    .build(),
            )
                .setEffects(
                    Effects(
                        createGainAudioProcessors(segment.gain),
                        emptyList(),
                    ),
                )
                .build()

            backgroundAudioSequenceBuilder.addItem(backgroundAudioItem)
        }

        val backgroundAudioSequence = backgroundAudioSequenceBuilder.build()

        return Composition.Builder(
            videoSequence,
            backgroundAudioSequence,
        ).build()
    }

    private fun Float.toStatusText(): String = when {
        this < 0.15f -> "Preparing project…"
        this < 0.35f -> "Building composition…"
        this < 0.60f -> "Rendering frames…"
        this < 0.85f -> "Merging audio and video…"
        this < 1f -> "Finalizing export…"
        else -> "Export completed"
    }

    private fun VideoCodec.toVideoMimeType(): String = when (this) {
        VideoCodec.H264 -> MimeTypes.VIDEO_H264
        VideoCodec.H265 -> MimeTypes.VIDEO_H265
    }

    private fun AudioCodec.toAudioMimeType(): String = when (this) {
        AudioCodec.AAC -> MimeTypes.AUDIO_AAC
    }

    private fun ExportFps.toFrameRate(): Int = when (this) {
        ExportFps.FPS_24 -> 24
        ExportFps.FPS_30 -> 30
        ExportFps.FPS_60 -> 60
    }

    private data class TimedSegment(
        val sourceIndex: Int,
        val uri: String,
        val mediaType: MediaType,
        val durationMs: Long,
        val trimStartMs: Long?,
        val trimEndMs: Long?,
        val activeOverlays: List<ExportTextOverlay>,
        val activeTransition: MediaTransitionWindow?,
    )

    private data class BackgroundAudioSegment(
        val uri: String,
        val startMs: Long,
        val endMs: Long,
        val gain: Float,
    )

    private fun buildBackgroundAudioSegments(
        backgroundUri: String,
        totalDurationMs: Long,
        sourceDurationMs: Long,
        shouldLoop: Boolean,
        audioMix: AudioMixRequest,
        overlays: List<ExportTextOverlay>,
    ): List<BackgroundAudioSegment> {
        if (totalDurationMs <= 0L || sourceDurationMs <= 0L) return emptyList()

        val effectiveTimelineDurationMs = if (shouldLoop) {
            totalDurationMs
        } else {
            minOf(totalDurationMs, sourceDurationMs)
        }

        val fadeInMs = audioMix.fadeInMs
            .coerceAtLeast(0L)
            .coerceAtMost(effectiveTimelineDurationMs)

        val fadeOutMs = audioMix.fadeOutMs
            .coerceAtLeast(0L)
            .coerceAtMost(effectiveTimelineDurationMs)

        val baseGain = audioMix.backgroundAudioVolume.coerceIn(0f, 1f)

        val timelineBoundaries = buildSet {
            add(0L)
            add(effectiveTimelineDurationMs)

            if (fadeInMs > 0L) {
                add(fadeInMs)
                add((fadeInMs * 0.5f).toLong())
            }

            if (fadeOutMs > 0L) {
                add(effectiveTimelineDurationMs - fadeOutMs)
                add(effectiveTimelineDurationMs - (fadeOutMs * 0.5f).toLong())
            }

            overlays.forEach { overlay ->
                add(overlay.startTimeMs.coerceIn(0L, effectiveTimelineDurationMs))
                overlay.endTimeMs?.let { end ->
                    add(end.coerceIn(0L, effectiveTimelineDurationMs))
                }
            }
        }
            .filter { it in 0L..effectiveTimelineDurationMs }
            .sorted()

        val result = mutableListOf<BackgroundAudioSegment>()

        for (index in 0 until timelineBoundaries.lastIndex) {
            val timelineStartMs = timelineBoundaries[index]
            val timelineEndMs = timelineBoundaries[index + 1]
            if (timelineEndMs <= timelineStartMs) continue

            val midpoint = timelineStartMs + ((timelineEndMs - timelineStartMs) / 2L)

            val overlayActive = overlays.any { overlay ->
                val overlayStart = overlay.startTimeMs
                val overlayEnd = overlay.endTimeMs ?: effectiveTimelineDurationMs
                overlay.text.isNotBlank() && midpoint in overlayStart until overlayEnd
            }

            val duckedGain = if (overlayActive) {
                (baseGain * 0.35f).coerceIn(0f, 1f)
            } else {
                baseGain
            }

            val fadeAdjustedGain = when {
                fadeInMs > 0L && midpoint < fadeInMs -> {
                    val ratio = midpoint.toFloat() / fadeInMs.toFloat()
                    (duckedGain * ratio).coerceIn(0f, duckedGain)
                }

                fadeOutMs > 0L && midpoint > (effectiveTimelineDurationMs - fadeOutMs) -> {
                    val remaining = (effectiveTimelineDurationMs - midpoint).coerceAtLeast(0L)
                    val ratio = remaining.toFloat() / fadeOutMs.toFloat()
                    (duckedGain * ratio).coerceIn(0f, duckedGain)
                }

                else -> duckedGain
            }

            if (!shouldLoop) {
                val localStartMs = timelineStartMs.coerceAtMost(sourceDurationMs)
                val localEndMs = timelineEndMs.coerceAtMost(sourceDurationMs)

                if (localEndMs > localStartMs) {
                    result += BackgroundAudioSegment(
                        uri = backgroundUri,
                        startMs = localStartMs,
                        endMs = localEndMs,
                        gain = fadeAdjustedGain,
                    )
                }
                continue
            }

            var cursor = timelineStartMs
            while (cursor < timelineEndMs) {
                val localStartMs = cursor % sourceDurationMs
                val availableUntilLoopEnd = sourceDurationMs - localStartMs
                val remainingInTimeline = timelineEndMs - cursor
                val chunkDuration = minOf(availableUntilLoopEnd, remainingInTimeline)

                val localEndMs = localStartMs + chunkDuration

                if (localEndMs > localStartMs) {
                    result += BackgroundAudioSegment(
                        uri = backgroundUri,
                        startMs = localStartMs,
                        endMs = localEndMs,
                        gain = fadeAdjustedGain,
                    )
                }

                cursor += chunkDuration
            }
        }

        return result.filter { it.endMs > it.startMs }
    }

    private fun segmentSequenceForEffects(
        sequenceItems: List<MediaExportSequenceItem>,
        overlays: List<ExportTextOverlay>,
        transitions: List<MediaTransitionWindow>,
    ): List<TimedSegment> {
        if (sequenceItems.isEmpty()) return emptyList()

        val result = mutableListOf<TimedSegment>()
        var timelineCursor = 0L

        sequenceItems.forEachIndexed { index, item ->
            val itemStart = timelineCursor
            val itemEnd = itemStart + item.durationMs

            val intro = transitions.firstOrNull {
                it.targetIndex == index && it.phase == MediaTransitionPhase.INTRO
            }
            val outro = transitions.firstOrNull {
                it.targetIndex == index && it.phase == MediaTransitionPhase.OUTRO
            }

            val boundaries = buildSet {
                add(itemStart)
                add(itemEnd)

                intro?.let { add((itemStart + it.durationMs).coerceAtMost(itemEnd)) }
                outro?.let { add((itemEnd - it.durationMs).coerceAtLeast(itemStart)) }

                overlays.forEach { overlay ->
                    val start = overlay.startTimeMs.coerceIn(itemStart, itemEnd)
                    val end = (overlay.endTimeMs ?: itemEnd).coerceIn(itemStart, itemEnd)
                    add(start)
                    add(end)
                }
            }.sorted()

            for (i in 0 until boundaries.lastIndex) {
                val segStart = boundaries[i]
                val segEnd = boundaries[i + 1]
                if (segEnd <= segStart) continue

                val activeOverlays = overlays.filter { overlay ->
                    val overlayStart = overlay.startTimeMs
                    val overlayEnd = overlay.endTimeMs ?: Long.MAX_VALUE
                    overlay.text.isNotBlank() &&
                            overlayStart < segEnd &&
                            overlayEnd > segStart
                }

                val activeTransition = when {
                    intro != null && segStart < itemStart + intro.durationMs -> intro
                    outro != null && segEnd > itemEnd - outro.durationMs -> outro
                    else -> null
                }

                val offsetInsideItem = segStart - itemStart
                val segmentDuration = segEnd - segStart

                val segmentTrimStartMs = when (item.mediaType) {
                    MediaType.IMAGE -> null
                    MediaType.VIDEO -> (item.trimStartMs ?: 0L) + offsetInsideItem
                }

                val segmentTrimEndMs = when (item.mediaType) {
                    MediaType.IMAGE -> null
                    MediaType.VIDEO -> (segmentTrimStartMs ?: 0L) + segmentDuration
                }

                result += TimedSegment(
                    sourceIndex = index,
                    uri = item.uri,
                    mediaType = item.mediaType,
                    durationMs = segmentDuration,
                    trimStartMs = segmentTrimStartMs,
                    trimEndMs = segmentTrimEndMs,
                    activeOverlays = activeOverlays,
                    activeTransition = activeTransition,
                )
            }

            timelineCursor = itemEnd
        }

        return result.filter { it.durationMs > 0L }
    }

    @OptIn(UnstableApi::class)
    private fun createGainAudioProcessors(
        gain: Float,
    ): List<AudioProcessor> {
        val normalizedGain = gain.coerceIn(0f, 1f)
        if (normalizedGain >= 0.999f) return emptyList()

        val gainProvider = DefaultGainProvider.Builder(normalizedGain).build()

        return listOf(
            GainProcessor(gainProvider),
        )
    }
}