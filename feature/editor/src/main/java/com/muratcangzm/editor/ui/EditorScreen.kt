package com.muratcangzm.editor.ui

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.muratcangzm.editor.ui.composable.AutoCaptionsSheet
import com.muratcangzm.editor.ui.composable.CaptionsPanel
import com.muratcangzm.editor.ui.composable.EditorBottomToolbar
import com.muratcangzm.media.domain.MediaThumbnailProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun EditorScreen(
    mediaUris: List<String>,
    onBack: () -> Unit,
    onOpenExport: (EditorContract.ExportPayload) -> Unit,
    projectId: String? = null,
    viewModel: EditorViewModel = koinViewModel(
        parameters = { parametersOf(mediaUris, projectId) }
    ),
    thumbnailProvider: MediaThumbnailProvider = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = false
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    val currentClipIndexRef = remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    LaunchedEffect(state.isMuted) {
        exoPlayer.volume = if (state.isMuted) 0f else 1f
    }

    val currentOnEvent = rememberUpdatedState(viewModel::onEvent)

    LaunchedEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    currentOnEvent.value(
                        EditorContract.Event.PlayerClipFinished(
                            clipIndex = currentClipIndexRef.intValue
                        )
                    )
                }
            }
        }
        exoPlayer.addListener(listener)
    }

    LaunchedEffect(state.isPlaying, exoPlayer) {
        while (isActive && state.isPlaying) {
            if (exoPlayer.playbackState == Player.STATE_READY && exoPlayer.isPlaying) {
                val rawPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
                val clips = viewModel.state.value.selectedMedia
                val clipIndex = currentClipIndexRef.intValue
                val trimStart = clips.getOrNull(clipIndex)?.trimStartMs ?: 0L
                val localPosition = (rawPosition - trimStart).coerceAtLeast(0L)
                currentOnEvent.value(EditorContract.Event.PlaybackPositionUpdate(localPosition))
            }
            delay(32L)
        }
    }

    val addMediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20),
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.onEvent(
                EditorContract.Event.AdditionalMediaSelected(uris.map { it.toString() })
            )
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onEvent(EditorContract.Event.AudioFileSelected(uri.toString()))
        }
    }

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                EditorContract.Effect.NavigateBack -> onBack()
                is EditorContract.Effect.NavigateExport -> onOpenExport(effect.payload)
                is EditorContract.Effect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
                is EditorContract.Effect.PreparePlayer -> {
                    val clips = viewModel.state.value.selectedMedia
                    if (effect.clipIndex in clips.indices) {
                        val clip = clips[effect.clipIndex]
                        currentClipIndexRef.intValue = effect.clipIndex
                        if (clip.isVideo) {
                            val mediaItem = MediaItem.fromUri(Uri.parse(clip.uri))
                            exoPlayer.stop()
                            exoPlayer.setMediaItem(mediaItem)
                            exoPlayer.playWhenReady = false
                            exoPlayer.prepare()
                            val seekTarget = (clip.trimStartMs ?: 0L) + effect.seekToMs
                            exoPlayer.seekTo(seekTarget.coerceAtLeast(0L))
                        } else {
                            exoPlayer.stop()
                            exoPlayer.clearMediaItems()
                        }
                    }
                }
                EditorContract.Effect.StartPlayer -> {
                    exoPlayer.playWhenReady = true
                    if (exoPlayer.playbackState == Player.STATE_IDLE) {
                        exoPlayer.prepare()
                    }
                }
                EditorContract.Effect.PausePlayer -> {
                    exoPlayer.playWhenReady = false
                }
                EditorContract.Effect.RequestMediaPicker -> {
                    addMediaPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                }
                EditorContract.Effect.RequestAudioFilePicker -> {
                    audioPickerLauncher.launch(arrayOf("audio/*"))
                }
            }
        }
    }

    EditorScreenContent(
        state = state,
        snackbarHostState = snackbarHostState,
        exoPlayer = exoPlayer,
        onEvent = viewModel::onEvent,
        thumbnailProvider = thumbnailProvider,
    )
}

@Composable
private fun EditorScreenContent(
    state: EditorContract.State,
    snackbarHostState: SnackbarHostState,
    exoPlayer: ExoPlayer,
    onEvent: (EditorContract.Event) -> Unit,
    thumbnailProvider: MediaThumbnailProvider,
) {
    val selectedMediaIndex = remember(state.selectedMedia, state.playbackPositionMs) {
        selectedMediaIndexForPosition(state.selectedMedia, state.playbackPositionMs)
    }
    val selectedMedia = state.selectedMedia.getOrNull(selectedMediaIndex)

    Scaffold(
        containerColor = EditorColors.ScreenBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(EditorColors.ScreenBackground)
                .padding(innerPadding),
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = EditorColors.Accent)
                    }
                }

                state.selectedMedia.isEmpty() && state.errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyEditorState(
                            title = "Editor unavailable",
                            body = state.errorMessage,
                        )
                    }
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        EditorTopBar(
                            state = state,
                            onClose = { onEvent(EditorContract.Event.BackClicked) },
                            onExport = { onEvent(EditorContract.Event.ExportClicked) },
                            onAspectRatioSelected = { ratio ->
                                onEvent(EditorContract.Event.AspectRatioSelected(ratio))
                            },
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        ) {
                            VideoPreviewArea(
                                state = state,
                                selectedMedia = selectedMedia,
                                exoPlayer = exoPlayer,
                                thumbnailProvider = thumbnailProvider,
                                onTogglePlay = { onEvent(EditorContract.Event.TogglePlayPause) },
                            )
                        }

                        PlaybackControlStrip(
                            state = state,
                            onTogglePlay = { onEvent(EditorContract.Event.TogglePlayPause) },
                            onSeekBack = { onEvent(EditorContract.Event.SeekBack) },
                            onSeekForward = { onEvent(EditorContract.Event.SeekForward) },
                        )

                        TimelineSection(
                            state = state,
                            selectedMediaIndex = selectedMediaIndex,
                            thumbnailProvider = thumbnailProvider,
                            onClipSelected = { index ->
                                onEvent(
                                    EditorContract.Event.SeekTo(
                                        clipStartPositionMs(state.selectedMedia, index)
                                    )
                                )
                            },
                            onReorderMedia = { from, to ->
                                onEvent(EditorContract.Event.ReorderMedia(from, to))
                            },
                            onMuteToggle = { onEvent(EditorContract.Event.ToggleMute) },
                            onAddMedia = { onEvent(EditorContract.Event.AddMediaClicked) },
                            onAddAudio = { onEvent(EditorContract.Event.AddAudioClicked) },
                            onRemoveAudio = { onEvent(EditorContract.Event.RemoveAudioTrack) },
                        )

                        EditorBottomToolbar(
                            activeTab = state.activeToolbarTab,
                            onTabSelected = { tab ->
                                onEvent(EditorContract.Event.ToolbarTabSelected(tab))
                            },
                            modifier = Modifier.navigationBarsPadding(),
                        )
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        CaptionsPanel(
                            visible = state.showCaptionsPanel && !state.showAutoCaptionsSheet,
                            onOptionSelected = { option ->
                                onEvent(EditorContract.Event.CaptionsOptionSelected(option))
                            },
                            onDismiss = { onEvent(EditorContract.Event.DismissCaptionsPanel) },
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )

                        AutoCaptionsSheet(
                            visible = state.showAutoCaptionsSheet,
                            config = state.autoCaptionsConfig,
                            onLanguageChanged = { lang ->
                                onEvent(EditorContract.Event.AutoCaptionsLanguageChanged(lang))
                            },
                            onFillerWordsToggled = { enabled ->
                                onEvent(EditorContract.Event.AutoCaptionsFillerWordsToggled(enabled))
                            },
                            onSourceChanged = { source ->
                                onEvent(EditorContract.Event.AutoCaptionsSourceChanged(source))
                            },
                            onGenerate = { onEvent(EditorContract.Event.GenerateAutoCaptions) },
                            onDismiss = { onEvent(EditorContract.Event.DismissAutoCaptionsSheet) },
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorTopBar(
    state: EditorContract.State,
    onClose: () -> Unit,
    onExport: () -> Unit,
    onAspectRatioSelected: (String) -> Unit = {},
) {
    var showResolutionMenu by remember { mutableStateOf(false) }
    var showAspectRatioMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Close",
                tint = EditorColors.TextPrimary,
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = state.projectName,
            color = EditorColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )

        Spacer(modifier = Modifier.weight(1f))

        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(EditorColors.Surface)
                    .border(1.dp, EditorColors.Border, RoundedCornerShape(8.dp))
                    .clickable { showAspectRatioMenu = !showAspectRatioMenu }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.selectedAspectRatio,
                    color = EditorColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = " \u25BE",
                    color = EditorColors.TextSecondary,
                    fontSize = 11.sp,
                )
            }

            DropdownMenu(
                expanded = showAspectRatioMenu,
                onDismissRequest = { showAspectRatioMenu = false },
            ) {
                state.availableAspectRatios.forEach { ratio ->
                    DropdownMenuItem(
                        text = { Text(ratio) },
                        onClick = {
                            onAspectRatioSelected(ratio)
                            showAspectRatioMenu = false
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(EditorColors.Surface)
                    .border(1.dp, EditorColors.Border, RoundedCornerShape(8.dp))
                    .clickable { showResolutionMenu = !showResolutionMenu }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "1080P",
                    color = EditorColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = " \u25BE",
                    color = EditorColors.TextSecondary,
                    fontSize = 11.sp,
                )
            }

            DropdownMenu(
                expanded = showResolutionMenu,
                onDismissRequest = { showResolutionMenu = false },
            ) {
                listOf("720P", "1080P", "4K").forEach { res ->
                    DropdownMenuItem(
                        text = { Text(res) },
                        onClick = { showResolutionMenu = false },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        IconButton(onClick = onExport) {
            Icon(
                imageVector = Icons.Outlined.Upload,
                contentDescription = "Export",
                tint = EditorColors.TextPrimary,
            )
        }
    }
}

@Composable
private fun VideoPreviewArea(
    state: EditorContract.State,
    selectedMedia: EditorContract.SelectedMediaItem?,
    exoPlayer: ExoPlayer,
    thumbnailProvider: MediaThumbnailProvider,
    onTogglePlay: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(onClick = onTogglePlay),
        contentAlignment = Alignment.Center,
    ) {
        when {
            selectedMedia == null -> {
                EmptyPreviewPlaceholder(isVideo = true, modifier = Modifier.fillMaxSize())
            }

            selectedMedia.isVideo -> {
                ExoPlayerSurface(
                    exoPlayer = exoPlayer,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            else -> {
                PreviewMediaThumbnail(
                    item = selectedMedia,
                    thumbnailProvider = thumbnailProvider,
                )
            }
        }

        if (state.textFields.isNotEmpty()) {
            val textValue = state.textFields.first().value.ifBlank {
                state.textFields.first().placeholder
            }
            if (textValue.isNotBlank()) {
                Text(
                    text = textValue,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = !state.isPlaying,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(400)),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                    )
                ),
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ExoPlayerSurface(
    exoPlayer: ExoPlayer,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            PlayerView(context).apply {
                player = exoPlayer
                useController = false
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                setBackgroundColor(android.graphics.Color.BLACK)
            }
        },
        update = { playerView ->
            if (playerView.player != exoPlayer) {
                playerView.player = exoPlayer
            }
        },
    )
}

@Composable
private fun PreviewMediaThumbnail(
    item: EditorContract.SelectedMediaItem,
    thumbnailProvider: MediaThumbnailProvider,
) {
    val bitmap by rememberThumbnailBitmap(
        uri = item.uri,
        sizePx = 640,
        thumbnailProvider = thumbnailProvider,
    )

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    } else {
        EmptyPreviewPlaceholder(isVideo = false, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun PlaybackControlStrip(
    state: EditorContract.State,
    onTogglePlay: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EditorColors.ScreenBackground)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = timelineTime(state.playbackPositionMs),
            color = EditorColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )

        Text(
            text = " / ",
            color = EditorColors.TextMuted,
            fontSize = 14.sp,
        )

        Text(
            text = timelineTime(state.totalDurationMs),
            color = EditorColors.TextSecondary,
            fontSize = 14.sp,
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SmallControlButton(
                icon = Icons.Outlined.FastRewind,
                onClick = onSeekBack,
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(EditorColors.Accent)
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Outlined.Pause
                    else Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF0A0F14),
                    modifier = Modifier.size(20.dp),
                )
            }

            SmallControlButton(
                icon = Icons.Outlined.FastForward,
                onClick = onSeekForward,
            )
        }
    }
}

@Composable
private fun SmallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = EditorColors.TextSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun TimelineSection(
    state: EditorContract.State,
    selectedMediaIndex: Int,
    thumbnailProvider: MediaThumbnailProvider,
    onClipSelected: (Int) -> Unit,
    onReorderMedia: (Int, Int) -> Unit,
    onMuteToggle: () -> Unit,
    onAddMedia: () -> Unit,
    onAddAudio: () -> Unit,
    onRemoveAudio: () -> Unit,
) {
    if (state.selectedMedia.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(EditorColors.TimelineSurface)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No media in timeline",
                color = EditorColors.TextMuted,
                fontSize = 13.sp,
            )
        }
        return
    }

    val scrollState = rememberScrollState()
    var viewportWidthPx by remember { mutableIntStateOf(0) }

    val clipWidths = remember(state.selectedMedia) {
        state.selectedMedia.map { item ->
            val durationSeconds = item.effectiveDurationMs / 1000f
            max(100f, durationSeconds * 60f)
        }
    }

    val totalTimelineWidth = remember(clipWidths) {
        val widthSum = clipWidths.fold(0f) { acc, w -> acc + w }
        max(400f, widthSum + 40f)
    }

    val playheadOffsetPx = remember(state.playbackPositionMs, clipWidths, state.selectedMedia) {
        computePlayheadOffset(state.selectedMedia, state.playbackPositionMs, clipWidths)
    }

    LaunchedEffect(playheadOffsetPx, viewportWidthPx, scrollState.maxValue) {
        if (viewportWidthPx <= 0) return@LaunchedEffect
        val target = (playheadOffsetPx - viewportWidthPx / 2f)
            .roundToInt()
            .coerceIn(0, scrollState.maxValue)
        if (abs(scrollState.value - target) > 20) {
            scrollState.animateScrollTo(target)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(EditorColors.TimelineSurface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onMuteToggle,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = if (state.isMuted) Icons.AutoMirrored.Outlined.VolumeOff
                    else Icons.AutoMirrored.Outlined.VolumeUp,
                    contentDescription = "Mute",
                    tint = if (state.isMuted) EditorColors.TextMuted else EditorColors.TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .onSizeChanged { viewportWidthPx = it.width }
                    .horizontalScroll(scrollState),
            ) {
                val timelineHeight = if (state.audioTrack != null) (64 + 4 + 40).dp else 64.dp

                Box(
                    modifier = Modifier
                        .width(totalTimelineWidth.dp)
                        .height(timelineHeight),
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            TimelineMediaRow(
                                clipWidths = clipWidths,
                                state = state,
                                selectedMediaIndex = selectedMediaIndex,
                                thumbnailProvider = thumbnailProvider,
                                onClipSelected = onClipSelected,
                                onReorderMedia = onReorderMedia,
                            )
                        }

                        if (state.audioTrack != null) {
                            Spacer(modifier = Modifier.height(4.dp))

                            AudioTrackRow(
                                audioTrack = state.audioTrack,
                                totalTimelineWidth = totalTimelineWidth,
                                onRemove = onRemoveAudio,
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(EditorColors.Accent)
                            .graphicsLayer { translationX = playheadOffsetPx },
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(EditorColors.Surface)
                    .border(1.dp, EditorColors.Border, RoundedCornerShape(10.dp))
                    .clickable(onClick = onAddMedia),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add clip",
                    tint = EditorColors.TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        if (state.audioTrack == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(EditorColors.Surface)
                        .border(1.dp, EditorColors.Border, RoundedCornerShape(8.dp))
                        .clickable(onClick = onAddAudio)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MusicNote,
                        contentDescription = null,
                        tint = EditorColors.TextMuted,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = "Add audio",
                        color = EditorColors.TextMuted,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioTrackRow(
    audioTrack: EditorContract.AudioTrackItem,
    totalTimelineWidth: Float,
    onRemove: () -> Unit,
) {
    val audioWidthFraction = if (audioTrack.durationMs > 0L) {
        (audioTrack.effectiveDurationMs / 1000f * 60f) / totalTimelineWidth
    } else 1f

    Box(
        modifier = Modifier
            .fillMaxWidth(audioWidthFraction.coerceIn(0.1f, 1f))
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(EditorColors.AccentAlt.copy(alpha = 0.18f))
            .border(1.dp, EditorColors.AccentAlt.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.MusicNote,
                contentDescription = null,
                tint = EditorColors.AccentAlt,
                modifier = Modifier.size(14.dp),
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = audioTrack.fileName,
                color = EditorColors.TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = timelineTime(audioTrack.effectiveDurationMs),
                color = EditorColors.TextSecondary,
                fontSize = 9.sp,
            )

            Spacer(modifier = Modifier.width(4.dp))

            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(EditorColors.Surface)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Remove audio",
                    tint = EditorColors.TextMuted,
                    modifier = Modifier.size(10.dp),
                )
            }
        }

        AudioWaveformDecoration(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.15f },
        )
    }
}

@Composable
private fun AudioWaveformDecoration(modifier: Modifier = Modifier) {
    val barCount = 40
    val bars = remember {
        List(barCount) { index ->
            val normalized = index.toFloat() / barCount
            val wave = kotlin.math.sin(normalized * Math.PI * 4).toFloat()
            0.25f + 0.75f * abs(wave)
        }
    }

    Row(
        modifier = modifier.padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEach { height ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(height)
                    .padding(horizontal = 0.5.dp)
                    .background(
                        EditorColors.AccentAlt,
                        RoundedCornerShape(1.dp),
                    ),
            )
        }
    }
}

@Composable
private fun TimelineMediaRow(
    clipWidths: List<Float>,
    state: EditorContract.State,
    selectedMediaIndex: Int,
    thumbnailProvider: MediaThumbnailProvider,
    onClipSelected: (Int) -> Unit,
    onReorderMedia: (Int, Int) -> Unit,
) {
    val density = LocalDensity.current
    val spacingPx = with(density) { 6.dp.toPx() }
    val clipWidthsPx = remember(clipWidths, density) {
        clipWidths.map { with(density) { it.dp.toPx() } }
    }

    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }

    fun findTargetIndex(centerX: Float): Int {
        var cursor = 0f
        clipWidthsPx.forEachIndexed { index, widthPx ->
            val itemCenter = cursor + widthPx / 2f
            if (centerX < itemCenter) return index
            cursor += widthPx + spacingPx
        }
        return clipWidthsPx.lastIndex
    }

    state.selectedMedia.forEachIndexed { index, item ->
        val currentTranslation = if (draggedIndex == index) dragOffsetPx else 0f
        val currentWidthPx = clipWidthsPx.getOrNull(index) ?: 0f

        TimelineClipCell(
            item = item,
            width = clipWidths.getOrNull(index) ?: 100f,
            selected = index == selectedMediaIndex,
            thumbnailProvider = thumbnailProvider,
            modifier = Modifier
                .zIndex(if (draggedIndex == index) 2f else 0f)
                .graphicsLayer {
                    translationX = currentTranslation
                    scaleX = if (draggedIndex == index) 1.04f else 1f
                    scaleY = if (draggedIndex == index) 1.04f else 1f
                    alpha = if (draggedIndex == index) 0.92f else 1f
                }
                .pointerInput(index, clipWidthsPx) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            draggedIndex = index
                            dragOffsetPx = 0f
                        },
                        onDragCancel = {
                            draggedIndex = -1
                            dragOffsetPx = 0f
                        },
                        onDragEnd = {
                            if (draggedIndex == index) {
                                var originalStart = 0f
                                repeat(index) { prev ->
                                    originalStart += clipWidthsPx[prev] + spacingPx
                                }
                                val draggedCenter =
                                    originalStart + dragOffsetPx + (currentWidthPx / 2f)
                                val targetIndex = findTargetIndex(draggedCenter)
                                if (targetIndex != index) {
                                    onReorderMedia(index, targetIndex)
                                }
                            }
                            draggedIndex = -1
                            dragOffsetPx = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (draggedIndex == index) {
                                dragOffsetPx += dragAmount.x
                            }
                        },
                    )
                },
            onClick = { onClipSelected(index) },
        )
    }
}

@Composable
private fun TimelineClipCell(
    item: EditorContract.SelectedMediaItem,
    width: Float,
    selected: Boolean,
    thumbnailProvider: MediaThumbnailProvider,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val frameCount = remember(width) {
        (width / 50f).roundToInt().coerceIn(2, 6)
    }

    val frameStrip by rememberTimelineStripBitmaps(
        context = context,
        item = item,
        frameCount = frameCount,
        thumbnailProvider = thumbnailProvider,
    )

    val borderColor by animateColorAsState(
        targetValue = if (selected) EditorColors.Accent else Color.Transparent,
        animationSpec = tween(200),
        label = "clipBorder",
    )

    Box(
        modifier = modifier
            .width(width.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick),
    ) {
        if (frameStrip.isNotEmpty()) {
            if (item.isVideo && frameStrip.size > 1) {
                Row(modifier = Modifier.fillMaxSize()) {
                    frameStrip.forEachIndexed { index, bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(end = if (index < frameStrip.lastIndex) 0.5.dp else 0.dp),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            } else {
                Image(
                    bitmap = frameStrip.first().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(EditorColors.Surface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (item.isVideo) Icons.Outlined.VideoLibrary
                    else Icons.Outlined.MovieCreation,
                    contentDescription = null,
                    tint = EditorColors.TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                    )
                )
                .padding(horizontal = 6.dp, vertical = 3.dp),
        ) {
            Text(
                text = item.durationLabel ?: if (item.isVideo) "Video" else "Photo",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }

        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(EditorColors.Accent),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(EditorColors.Accent),
            )
        }
    }
}

@Composable
private fun EmptyEditorState(
    title: String,
    body: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(EditorColors.Surface)
                .border(1.dp, EditorColors.Border, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.MovieCreation,
                contentDescription = null,
                tint = EditorColors.Accent,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = title,
            color = EditorColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = body,
            color = EditorColors.TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EmptyPreviewPlaceholder(
    isVideo: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = listOf(Color(0xFF171B24), Color(0xFF10131A)),
            )
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isVideo) Icons.Outlined.VideoLibrary
            else Icons.Outlined.MovieCreation,
            contentDescription = null,
            tint = EditorColors.TextMuted,
            modifier = Modifier.size(42.dp),
        )
    }
}

@Composable
private fun rememberThumbnailBitmap(
    uri: String,
    sizePx: Int,
    thumbnailProvider: MediaThumbnailProvider,
): State<Bitmap?> {
    return produceState<Bitmap?>(initialValue = null, key1 = uri, key2 = sizePx) {
        value = withContext(Dispatchers.IO) {
            thumbnailProvider.loadThumbnail(uri = uri, sizePx = sizePx)
        }
    }
}

@Composable
private fun rememberTimelineStripBitmaps(
    context: Context,
    item: EditorContract.SelectedMediaItem,
    frameCount: Int,
    thumbnailProvider: MediaThumbnailProvider,
): State<List<Bitmap>> {
    val requestKey = remember(item.uri, item.trimStartMs, item.trimEndMs, frameCount) {
        "${item.uri}|${item.trimStartMs}|${item.trimEndMs}|$frameCount"
    }

    return produceState(initialValue = emptyList<Bitmap>(), key1 = requestKey) {
        value = withContext(Dispatchers.IO) {
            loadTimelineStripBitmaps(context, item, frameCount, thumbnailProvider)
        }
    }
}

private fun loadTimelineStripBitmaps(
    context: Context,
    item: EditorContract.SelectedMediaItem,
    frameCount: Int,
    thumbnailProvider: MediaThumbnailProvider,
): List<Bitmap> {
    val safeFrameCount = frameCount.coerceIn(1, 6)
    val thumbSize = 220

    if (!item.isVideo) {
        return listOfNotNull(
            thumbnailProvider.loadThumbnail(uri = item.uri, sizePx = thumbSize)
        )
    }

    val parsedUri = runCatching { item.uri.toUri() }.getOrNull() ?: return emptyList()
    val trimStart = item.trimStartMs ?: 0L
    val trimEnd = item.trimEndMs ?: (item.sourceDurationMs ?: item.effectiveDurationMs)
    val effectiveDuration = (trimEnd - trimStart).coerceAtLeast(600L)

    val retriever = MediaMetadataRetriever()

    return try {
        retriever.setDataSource(context, parsedUri)

        buildList {
            repeat(safeFrameCount) { index ->
                val fraction = if (safeFrameCount == 1) 0.5f
                else index.toFloat() / (safeFrameCount - 1).toFloat()

                val timeMs = trimStart + (effectiveDuration * fraction).toLong()
                val timeUs = timeMs * 1_000L

                val bitmap = retriever.getScaledFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    thumbSize,
                    thumbSize,
                )
                if (bitmap != null) add(bitmap)
            }
        }.ifEmpty {
            listOfNotNull(
                thumbnailProvider.loadThumbnail(uri = item.uri, sizePx = thumbSize)
            )
        }
    } catch (_: Throwable) {
        listOfNotNull(
            thumbnailProvider.loadThumbnail(uri = item.uri, sizePx = thumbSize)
        )
    } finally {
        runCatching { retriever.release() }
    }
}

private fun computePlayheadOffset(
    items: List<EditorContract.SelectedMediaItem>,
    positionMs: Long,
    clipWidths: List<Float>,
): Float {
    if (items.isEmpty() || clipWidths.isEmpty()) return 0f
    val totalDuration = items.fold(0L) { acc, item -> acc + item.effectiveDurationMs }
    if (totalDuration <= 0L) return 0f

    var cursor = 0L
    var pixelCursor = 0f
    val spacing = 6f

    items.forEachIndexed { index, item ->
        val clipDuration = item.effectiveDurationMs
        val clipWidth = clipWidths.getOrElse(index) { 100f }

        if (positionMs < cursor + clipDuration) {
            val localProgress = (positionMs - cursor).toFloat() / clipDuration.toFloat()
            return pixelCursor + (clipWidth * localProgress.coerceIn(0f, 1f))
        }

        cursor += clipDuration
        pixelCursor += clipWidth + spacing
    }

    return pixelCursor - spacing
}

private fun selectedMediaIndexForPosition(
    items: List<EditorContract.SelectedMediaItem>,
    positionMs: Long,
): Int {
    if (items.isEmpty()) return 0
    var cursor = 0L
    items.forEachIndexed { index, item ->
        val end = cursor + item.effectiveDurationMs
        if (positionMs < end) return index
        cursor = end
    }
    return items.lastIndex
}

private fun clipStartPositionMs(
    items: List<EditorContract.SelectedMediaItem>,
    index: Int,
): Long {
    if (index <= 0) return 0L
    return items.take(index).sumOf { it.effectiveDurationMs }
}

private fun timelineTime(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}

private object EditorColors {
    val ScreenBackground = Color(0xFF0E1117)
    val Surface = Color(0xFF161B22)
    val TimelineSurface = Color(0xFF0D1117)
    val Border = Color(0xFF21262D)

    val Accent = Color(0xFF00D4AA)
    val AccentAlt = Color(0xFF7F73FF)

    val TextPrimary = Color(0xFFF0F6FC)
    val TextSecondary = Color(0xFF8B949E)
    val TextMuted = Color(0xFF484F58)

    val Success = Color(0xFF3FB950)
    val Warning = Color(0xFFD29922)
    val Error = Color(0xFFF85149)
}
