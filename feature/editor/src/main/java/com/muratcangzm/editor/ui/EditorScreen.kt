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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.outlined.Check
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.outlined.TextFields
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
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
                    if (state.showRenameDialog) {
                        RenameProjectDialog(
                            currentName = state.projectName,
                            onConfirm = { onEvent(EditorContract.Event.ProjectNameChanged(it)) },
                            onDismiss = { onEvent(EditorContract.Event.DismissRenameDialog) },
                        )
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        EditorTopBar(
                            state = state,
                            onClose = { onEvent(EditorContract.Event.BackClicked) },
                            onExport = { onEvent(EditorContract.Event.ExportClicked) },
                            onProjectNameClicked = { onEvent(EditorContract.Event.ProjectNameClicked) },
                            onAspectRatioSelected = { ratio ->
                                onEvent(EditorContract.Event.AspectRatioSelected(ratio))
                            },
                            onResolutionSelected = { resolution ->
                                onEvent(EditorContract.Event.ResolutionSelected(resolution))
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
                                onTextOverlayPositionChanged = { id, x, y ->
                                    onEvent(EditorContract.Event.TextOverlayPositionChanged(id, x, y))
                                },
                                onTextOverlayRotationChanged = { id, rotation ->
                                    onEvent(EditorContract.Event.TextOverlayRotationChanged(id, rotation))
                                },
                                onTextOverlayDoubleTap = { id ->
                                    onEvent(EditorContract.Event.ShowDurationPicker(id))
                                },
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
                            onRemoveAudio = { id -> onEvent(EditorContract.Event.RemoveAudioTrack(id)) },
                            onOpenMediaTrim = { uri -> onEvent(EditorContract.Event.OpenMediaTrimmer(uri)) },
                            onOpenAudioTrim = { id -> onEvent(EditorContract.Event.OpenAudioTrimmer(id)) },
                            onAudioOffsetChanged = { id, offset ->
                                onEvent(EditorContract.Event.AudioOffsetChanged(id, offset))
                            },
                            onEditTextOverlay = { id ->
                                onEvent(EditorContract.Event.EditTextOverlay(id))
                            },
                            onDeleteTextOverlay = { id ->
                                onEvent(EditorContract.Event.DeleteTextOverlay(id))
                            },
                            onSeekToPosition = { posMs ->
                                onEvent(EditorContract.Event.SeekTo(posMs))
                            },
                        )

                        val trimmingMedia = state.trimmingMediaUri?.let { uri ->
                            state.selectedMedia.find { it.uri == uri }
                        }
                        if (trimmingMedia != null && trimmingMedia.canTrim) {
                            TrimBar(
                                label = trimmingMedia.fileName,
                                durationMs = trimmingMedia.sourceDurationMs ?: trimmingMedia.effectiveDurationMs,
                                startMs = trimmingMedia.trimStartMs ?: 0L,
                                endMs = trimmingMedia.trimEndMs ?: trimmingMedia.sourceDurationMs ?: trimmingMedia.effectiveDurationMs,
                                onTrimChanged = { start, end ->
                                    onEvent(EditorContract.Event.TrimChanged(trimmingMedia.uri, start, end))
                                },
                                onDismiss = { onEvent(EditorContract.Event.DismissTrimmer) },
                            )
                        }

                        val trimmingAudio = state.trimmingAudioId?.let { id ->
                            state.audioTracks.find { it.id == id }
                        }
                        if (trimmingAudio != null) {
                            TrimBar(
                                label = trimmingAudio.fileName,
                                durationMs = trimmingAudio.durationMs,
                                startMs = trimmingAudio.trimStartMs,
                                endMs = trimmingAudio.trimEndMs ?: trimmingAudio.durationMs,
                                onTrimChanged = { start, end ->
                                    onEvent(EditorContract.Event.AudioTrimChanged(trimmingAudio.id, start, end))
                                },
                                onDismiss = { onEvent(EditorContract.Event.DismissTrimmer) },
                            )
                        }

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

                    if (state.showTextOverlayInput) {
                        val editingOverlay = state.editingTextOverlayId?.let { id ->
                            state.textOverlays.find { it.id == id }
                        }
                        TextOverlayInputDialog(
                            editingOverlay = editingOverlay,
                            totalDurationMs = state.totalDurationMs,
                            onConfirm = { text, gravity, durationMs ->
                                if (editingOverlay != null) {
                                    onEvent(
                                        EditorContract.Event.UpdateTextOverlay(
                                            overlayId = editingOverlay.id,
                                            text = text,
                                            gravity = gravity,
                                            durationMs = durationMs,
                                        )
                                    )
                                } else {
                                    onEvent(
                                        EditorContract.Event.ConfirmTextOverlay(
                                            text = text,
                                            gravity = gravity,
                                            durationMs = durationMs,
                                        )
                                    )
                                }
                            },
                            onDismiss = {
                                onEvent(EditorContract.Event.DismissTextOverlayInput)
                            },
                        )
                    }

                    // Duration picker dialog on double-tap
                    state.showDurationPickerOverlayId?.let { overlayId ->
                        val overlay = state.textOverlays.find { it.id == overlayId }
                        if (overlay != null) {
                            DurationPickerDialog(
                                currentDurationMs = overlay.endMs - overlay.startMs,
                                maxDurationMs = state.totalDurationMs,
                                onConfirm = { newDurationMs ->
                                    onEvent(
                                        EditorContract.Event.TextOverlayDurationChanged(
                                            overlayId = overlayId,
                                            durationMs = newDurationMs,
                                        )
                                    )
                                },
                                onDismiss = {
                                    onEvent(EditorContract.Event.DismissDurationPicker)
                                },
                            )
                        }
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
    onProjectNameClicked: () -> Unit = {},
    onAspectRatioSelected: (String) -> Unit = {},
    onResolutionSelected: (EditorResolutionOption) -> Unit = {},
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
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onProjectNameClicked)
                .padding(vertical = 4.dp, horizontal = 2.dp),
        )

        Spacer(modifier = Modifier.width(8.dp))

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
                    text = state.selectedResolution.label,
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
                state.availableResolutions.forEach { resolution ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = resolution.label,
                                fontWeight = if (resolution == state.selectedResolution) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        onClick = {
                            onResolutionSelected(resolution)
                            showResolutionMenu = false
                        },
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
    onTextOverlayPositionChanged: (String, Float, Float) -> Unit = { _, _, _ -> },
    onTextOverlayRotationChanged: (String, Float) -> Unit = { _, _ -> },
    onTextOverlayDoubleTap: (String) -> Unit = {},
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
                PreviewMediaThumbnail(
                    item = selectedMedia,
                    thumbnailProvider = thumbnailProvider,
                )

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

        // Show active text overlays at current playback position — draggable
        var previewSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { previewSize = it },
        ) {
            state.textOverlays.forEach { overlay ->
                if (state.playbackPositionMs in overlay.startMs..overlay.endMs) {
                    DraggableTextOverlay(
                        overlay = overlay,
                        previewSize = previewSize,
                        onPositionChanged = { x, y ->
                            onTextOverlayPositionChanged(overlay.id, x, y)
                        },
                        onRotationChanged = { rotation ->
                            onTextOverlayRotationChanged(overlay.id, rotation)
                        },
                        onDoubleTap = {
                            onTextOverlayDoubleTap(overlay.id)
                        },
                    )
                }
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
    onRemoveAudio: (String) -> Unit,
    onOpenMediaTrim: (String) -> Unit,
    onOpenAudioTrim: (String) -> Unit,
    onAudioOffsetChanged: (String, Long) -> Unit = { _, _ -> },
    onEditTextOverlay: (String) -> Unit = {},
    onDeleteTextOverlay: (String) -> Unit = {},
    onSeekToPosition: (Long) -> Unit = {},
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

    val density = LocalDensity.current
    var playheadPx by remember { mutableFloatStateOf(0f) }

    val computedPx = remember(state.playbackPositionMs, clipWidths, state.selectedMedia) {
        val dpVal = computePlayheadOffset(state.selectedMedia, state.playbackPositionMs, clipWidths)
        with(density) { dpVal.dp.toPx() }
    }
    playheadPx = computedPx

    LaunchedEffect(computedPx, viewportWidthPx, scrollState.maxValue) {
        if (viewportWidthPx <= 0) return@LaunchedEffect
        val target = (computedPx - viewportWidthPx / 2f)
            .roundToInt()
            .coerceIn(0, scrollState.maxValue)
        if (abs(scrollState.value - target) > viewportWidthPx * 0.05f) {
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
                val textOverlayCount = state.textOverlays.size
                val audioTrackCount = state.audioTracks.size
                val timelineHeight = (64 + textOverlayCount * 32 + audioTrackCount * 44).dp

                Box(
                    modifier = Modifier
                        .width(totalTimelineWidth.dp)
                        .height(timelineHeight)
                        .pointerInput(state.totalDurationMs, totalTimelineWidth) {
                            detectTapGestures { offset ->
                                val fraction = (offset.x / (totalTimelineWidth * density.density))
                                    .coerceIn(0f, 1f)
                                val seekMs = (fraction * state.totalDurationMs).toLong()
                                onSeekToPosition(seekMs)
                            }
                        },
                ) {
                    Column {
                        state.textOverlays.forEach { overlay ->
                            TextOverlayTrackRow(
                                overlay = overlay,
                                totalTimelineWidth = totalTimelineWidth,
                                totalDurationMs = state.totalDurationMs,
                                onEdit = { onEditTextOverlay(overlay.id) },
                                onDelete = { onDeleteTextOverlay(overlay.id) },
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }

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
                                onOpenTrim = onOpenMediaTrim,
                            )
                        }

                        state.audioTracks.forEach { audioTrack ->
                            Spacer(modifier = Modifier.height(4.dp))

                            AudioTrackRow(
                                audioTrack = audioTrack,
                                totalTimelineWidth = totalTimelineWidth,
                                totalDurationMs = state.totalDurationMs,
                                onRemove = { onRemoveAudio(audioTrack.id) },
                                onTrim = { onOpenAudioTrim(audioTrack.id) },
                                onOffsetChanged = { newOffset ->
                                    onAudioOffsetChanged(audioTrack.id, newOffset)
                                },
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(playheadPx.roundToInt(), 0) }
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(EditorColors.Accent),
                    )
                }
            }

            Column(
                modifier = Modifier.padding(start = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
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

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EditorColors.Surface)
                        .border(1.dp, EditorColors.Border, RoundedCornerShape(10.dp))
                        .clickable(onClick = onAddAudio),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MusicNote,
                        contentDescription = "Add audio",
                        tint = EditorColors.AccentAlt,
                        modifier = Modifier.size(18.dp),
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
    totalDurationMs: Long,
    onRemove: () -> Unit,
    onTrim: () -> Unit,
    onOffsetChanged: (Long) -> Unit,
) {
    val audioWidthDp = (audioTrack.effectiveDurationMs / 1000f * 60f)
        .coerceIn(60f, totalTimelineWidth)

    val offsetFraction = if (totalDurationMs > 0L) {
        audioTrack.offsetMs.toFloat() / totalDurationMs
    } else 0f
    val offsetDp = totalTimelineWidth * offsetFraction

    var localOffsetDp by remember(offsetDp) { mutableFloatStateOf(offsetDp) }

    Box(
        modifier = Modifier
            .width(totalTimelineWidth.dp)
            .height(40.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(start = localOffsetDp.dp)
                .width(audioWidthDp.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(EditorColors.AccentAlt.copy(alpha = 0.18f))
                .border(1.dp, EditorColors.AccentAlt.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .pointerInput(totalTimelineWidth, totalDurationMs) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (totalTimelineWidth > 0f && totalDurationMs > 0L) {
                                val newMs = ((localOffsetDp / totalTimelineWidth) * totalDurationMs).toLong()
                                onOffsetChanged(newMs.coerceIn(0L, totalDurationMs))
                            }
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        val deltaDp = dragAmount / density
                        localOffsetDp = (localOffsetDp + deltaDp)
                            .coerceIn(0f, totalTimelineWidth - audioWidthDp)
                    }
                }
                .clickable(onClick = onTrim),
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
    onOpenTrim: (String) -> Unit,
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
            onDoubleTap = { if (item.canTrim) onOpenTrim(item.uri) },
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
    onDoubleTap: () -> Unit = {},
) {
    val context = LocalContext.current
    val frameCount = remember(width) {
        (width / 60f).roundToInt().coerceIn(2, 20)
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
            .pointerInput(onClick, onDoubleTap) {
                detectTapGestures(
                    onTap = { onClick() },
                    onDoubleTap = { onDoubleTap() },
                )
            },
    ) {
        if (frameStrip.isNotEmpty()) {
            if (item.isVideo && frameStrip.size > 1) {
                Row(modifier = Modifier.fillMaxSize()) {
                    frameStrip.forEach { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
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
    val safeFrameCount = frameCount.coerceIn(1, 20)
    val thumbSize = 160

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

@Composable
private fun TrimBar(
    label: String,
    durationMs: Long,
    startMs: Long,
    endMs: Long,
    onTrimChanged: (Long, Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var localStart by remember(startMs) { mutableLongStateOf(startMs) }
    var localEnd by remember(endMs) { mutableLongStateOf(endMs) }

    val startFraction = if (durationMs > 0) localStart.toFloat() / durationMs else 0f
    val endFraction = if (durationMs > 0) localEnd.toFloat() / durationMs else 1f

    val hasChanges = localStart != startMs || localEnd != endMs

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(EditorColors.Surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.LocalFireDepartment,
                contentDescription = null,
                tint = EditorColors.Accent,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Trim: $label",
                color = EditorColors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${timelineTime(localStart)} - ${timelineTime(localEnd)}",
                color = if (hasChanges) EditorColors.Accent else EditorColors.TextSecondary,
                fontSize = 11.sp,
            )
            Spacer(modifier = Modifier.width(8.dp))

            if (hasChanges) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(EditorColors.Accent)
                        .clickable {
                            onTrimChanged(localStart, localEnd)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Apply trim",
                        tint = EditorColors.ScreenBackground,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(EditorColors.Border)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close trim",
                    tint = EditorColors.TextPrimary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        var trackWidthPx by remember { mutableIntStateOf(0) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(EditorColors.ScreenBackground)
                .onSizeChanged { trackWidthPx = it.width },
        ) {
            val density = LocalDensity.current
            val leftDp = with(density) { (trackWidthPx * startFraction).toDp() }
            val rightDp = with(density) { (trackWidthPx * (1f - endFraction)).toDp() }
            val hatchColor = Color.White.copy(alpha = 0.15f)

            if (startFraction > 0f) {
                Canvas(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(leftDp)
                        .fillMaxHeight(),
                ) {
                    val step = 8.dp.toPx()
                    var x = 0f
                    while (x < size.width + size.height) {
                        drawLine(
                            color = hatchColor,
                            start = Offset(x, 0f),
                            end = Offset(x - size.height, size.height),
                            strokeWidth = 2.dp.toPx(),
                        )
                        x += step
                    }
                }
            }

            if (endFraction < 1f) {
                Canvas(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(rightDp)
                        .fillMaxHeight(),
                ) {
                    val step = 8.dp.toPx()
                    var x = 0f
                    while (x < size.width + size.height) {
                        drawLine(
                            color = hatchColor,
                            start = Offset(x, 0f),
                            end = Offset(x - size.height, size.height),
                            strokeWidth = 2.dp.toPx(),
                        )
                        x += step
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = leftDp, end = rightDp)
                    .background(EditorColors.Accent.copy(alpha = 0.3f)),
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = leftDp)
                    .width(20.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                    .background(EditorColors.Accent)
                    .pointerInput(durationMs) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            if (trackWidthPx > 0) {
                                val delta = (dragAmount / trackWidthPx) * durationMs
                                localStart = (localStart + delta.toLong())
                                    .coerceIn(0L, localEnd - 300L)
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(16.dp)
                        .background(EditorColors.ScreenBackground, RoundedCornerShape(1.dp)),
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = rightDp)
                    .width(20.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                    .background(EditorColors.Accent)
                    .pointerInput(durationMs) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            if (trackWidthPx > 0) {
                                val delta = (dragAmount / trackWidthPx) * durationMs
                                localEnd = (localEnd + delta.toLong())
                                    .coerceIn(localStart + 300L, durationMs)
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(16.dp)
                        .background(EditorColors.ScreenBackground, RoundedCornerShape(1.dp)),
                )
            }
        }
    }
}

@Composable
private fun RenameProjectDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var textState by remember {
        mutableStateOf(
            TextFieldValue(
                text = currentName,
                selection = TextRange(0, currentName.length),
            )
        )
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(EditorColors.Surface)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Rename Project",
                    color = EditorColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(16.dp))

                BasicTextField(
                    value = textState,
                    onValueChange = { textState = it.copy(text = it.text.take(60)) },
                    singleLine = true,
                    cursorBrush = SolidColor(EditorColors.Accent),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = EditorColors.TextPrimary,
                        fontSize = 15.sp,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EditorColors.ScreenBackground)
                        .border(1.dp, EditorColors.Accent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(EditorColors.Border)
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Cancel",
                            color = EditorColors.TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(EditorColors.Accent)
                            .clickable {
                                val trimmed = textState.text.trim()
                                if (trimmed.isNotBlank()) onConfirm(trimmed) else onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Rename",
                            color = EditorColors.ScreenBackground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
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

@Composable
private fun TextOverlayTrackRow(
    overlay: EditorContract.TextOverlayItem,
    totalTimelineWidth: Float,
    totalDurationMs: Long,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    if (totalDurationMs <= 0L) return

    val startFraction = overlay.startMs.toFloat() / totalDurationMs
    val durationFraction = (overlay.endMs - overlay.startMs).toFloat() / totalDurationMs
    val offsetDp = totalTimelineWidth * startFraction
    val widthDp = (totalTimelineWidth * durationFraction).coerceAtLeast(40f)

    Box(
        modifier = Modifier
            .width(totalTimelineWidth.dp)
            .height(28.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(start = offsetDp.dp)
                .width(widthDp.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF00D4AA).copy(alpha = 0.20f))
                .border(1.dp, Color(0xFF00D4AA).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .clickable(onClick = onEdit),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.TextFields,
                    contentDescription = null,
                    tint = Color(0xFF00D4AA),
                    modifier = Modifier.size(12.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = overlay.text,
                    color = EditorColors.TextPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(EditorColors.Surface)
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Remove text",
                        tint = EditorColors.TextMuted,
                        modifier = Modifier.size(9.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TextOverlayInputDialog(
    editingOverlay: EditorContract.TextOverlayItem?,
    totalDurationMs: Long,
    onConfirm: (String, EditorContract.TextOverlayGravity, Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val defaultDuration = editingOverlay?.let { it.endMs - it.startMs }
        ?: (totalDurationMs * 0.30).toLong().coerceIn(1_000L, 5_000L)

    var textState by remember(editingOverlay) {
        mutableStateOf(
            TextFieldValue(
                text = editingOverlay?.text ?: "",
                selection = TextRange(0, editingOverlay?.text?.length ?: 0),
            )
        )
    }
    var selectedGravity by remember(editingOverlay) {
        mutableStateOf(editingOverlay?.gravity ?: EditorContract.TextOverlayGravity.CENTER)
    }
    var durationSeconds by remember(editingOverlay) {
        mutableStateOf((defaultDuration / 1000f).let { "%.1f".format(it) })
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 28.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(EditorColors.Surface)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (editingOverlay != null) "Edit Text" else "Add Text",
                    color = EditorColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(20.dp))

                BasicTextField(
                    value = textState,
                    onValueChange = { textState = it.copy(text = it.text.take(100)) },
                    singleLine = false,
                    maxLines = 3,
                    cursorBrush = SolidColor(EditorColors.Accent),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = EditorColors.TextPrimary,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EditorColors.ScreenBackground)
                        .border(
                            1.dp,
                            EditorColors.Accent.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.Center) {
                            if (textState.text.isEmpty()) {
                                Text(
                                    text = "Type your text here...",
                                    color = EditorColors.TextMuted,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                )
                            }
                            innerTextField()
                        }
                    },
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Duration input
                Text(
                    text = "Duration (seconds)",
                    color = EditorColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )

                Spacer(modifier = Modifier.height(8.dp))

                BasicTextField(
                    value = durationSeconds,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d{0,3}\\.?\\d{0,1}$"))) {
                            durationSeconds = input
                        }
                    },
                    singleLine = true,
                    cursorBrush = SolidColor(EditorColors.Accent),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = EditorColors.TextPrimary,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(EditorColors.ScreenBackground)
                        .border(
                            1.dp,
                            EditorColors.Border,
                            RoundedCornerShape(12.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.Center) {
                            if (durationSeconds.isEmpty()) {
                                Text(
                                    text = "e.g. 3.0",
                                    color = EditorColors.TextMuted,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                )
                            }
                            innerTextField()
                        }
                    },
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Position",
                    color = EditorColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    EditorContract.TextOverlayGravity.entries.forEach { gravity ->
                        val isSelected = gravity == selectedGravity
                        val label = when (gravity) {
                            EditorContract.TextOverlayGravity.TOP_CENTER -> "Top"
                            EditorContract.TextOverlayGravity.CENTER -> "Center"
                            EditorContract.TextOverlayGravity.BOTTOM_CENTER -> "Bottom"
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) EditorColors.Accent.copy(alpha = 0.15f)
                                    else EditorColors.ScreenBackground,
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) EditorColors.Accent
                                    else EditorColors.Border,
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .clickable { selectedGravity = gravity }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) EditorColors.Accent
                                else EditorColors.TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold
                                else FontWeight.Normal,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(EditorColors.Border)
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Cancel",
                            color = EditorColors.TextSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (textState.text.isNotBlank()) EditorColors.Accent
                                else EditorColors.Accent.copy(alpha = 0.4f),
                            )
                            .clickable {
                                val trimmed = textState.text.trim()
                                val durMs = (durationSeconds.toFloatOrNull() ?: 3f)
                                    .coerceIn(0.5f, totalDurationMs / 1000f)
                                    .let { (it * 1000).toLong() }
                                if (trimmed.isNotBlank()) onConfirm(trimmed, selectedGravity, durMs)
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (editingOverlay != null) "Update" else "Add",
                            color = EditorColors.ScreenBackground,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DraggableTextOverlay(
    overlay: EditorContract.TextOverlayItem,
    previewSize: androidx.compose.ui.unit.IntSize,
    onPositionChanged: (Float, Float) -> Unit,
    onRotationChanged: (Float) -> Unit,
    onDoubleTap: () -> Unit,
) {
    val density = LocalDensity.current

    var currentXFraction by remember(overlay.id) { mutableFloatStateOf(overlay.offsetXFraction) }
    var currentYFraction by remember(overlay.id) { mutableFloatStateOf(overlay.offsetYFraction) }
    var currentRotation by remember(overlay.id) { mutableFloatStateOf(overlay.rotationDegrees) }

    // Sync from state when not dragging
    LaunchedEffect(overlay.offsetXFraction, overlay.offsetYFraction, overlay.rotationDegrees) {
        currentXFraction = overlay.offsetXFraction
        currentYFraction = overlay.offsetYFraction
        currentRotation = overlay.rotationDegrees
    }

    if (previewSize.width <= 0 || previewSize.height <= 0) return

    val offsetX = currentXFraction * previewSize.width
    val offsetY = currentYFraction * previewSize.height

    val transformState = rememberTransformableState { _, pan, rotationChange ->
        val newX = (currentXFraction + pan.x / previewSize.width).coerceIn(0.05f, 0.95f)
        val newY = (currentYFraction + pan.y / previewSize.height).coerceIn(0.05f, 0.95f)
        currentXFraction = newX
        currentYFraction = newY
        onPositionChanged(newX, newY)

        currentRotation += rotationChange
        onRotationChanged(currentRotation)
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.TopStart,
    ) {
        Text(
            text = overlay.text,
            color = Color.White,
            fontSize = overlay.textSizeSp.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .offset {
                    IntOffset(
                        (offsetX - 60 * density.density).roundToInt(),
                        offsetY.roundToInt(),
                    )
                }
                .graphicsLayer {
                    rotationZ = currentRotation
                }
                .pointerInput(overlay.id) {
                    detectTapGestures(
                        onDoubleTap = { onDoubleTap() },
                    )
                }
                .transformable(transformState)
                .background(
                    Color.Black.copy(alpha = 0.4f),
                    RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun DurationPickerDialog(
    currentDurationMs: Long,
    maxDurationMs: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var durationText by remember {
        mutableStateOf("%.1f".format(currentDurationMs / 1000f))
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(EditorColors.Surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Text Duration",
                color = EditorColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "How long should this text appear? (seconds)",
                color = EditorColors.TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            BasicTextField(
                value = durationText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d{0,3}\\.?\\d{0,1}$"))) {
                        durationText = input
                    }
                },
                singleLine = true,
                cursorBrush = SolidColor(EditorColors.Accent),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = EditorColors.TextPrimary,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(EditorColors.ScreenBackground)
                    .border(1.dp, EditorColors.Accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EditorColors.Border)
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Cancel", color = EditorColors.TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EditorColors.Accent)
                        .clickable {
                            val durMs = (durationText.toFloatOrNull() ?: (currentDurationMs / 1000f))
                                .coerceIn(0.5f, maxDurationMs / 1000f)
                                .let { (it * 1000).toLong() }
                            onConfirm(durMs)
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Apply", color = EditorColors.ScreenBackground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
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

}