package com.changecut.feature.editor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.changecut.feature.editor.ai.AiAutoEditScreen
import com.changecut.feature.editor.ai.AiCaptionScreen
import com.changecut.feature.editor.ai.AiVoiceOverScreen
import com.changecut.feature.editor.audio.AudioTrackScreen
import com.changecut.feature.editor.effects.EffectScreen
import com.changecut.feature.editor.effects.TransitionPickerScreen
import com.changecut.feature.editor.keyframe.KeyframeEditorScreen
import com.changecut.feature.editor.preview.GpuPreviewView
import com.changecut.feature.editor.pro.AnimationPickerScreen
import com.changecut.feature.editor.pro.AudioEQScreen
import com.changecut.feature.editor.pro.AdjustmentLayerScreen
import com.changecut.feature.editor.pro.ChromaKeyScreen
import com.changecut.feature.editor.pro.ColorGradingScreen
import com.changecut.feature.editor.pro.MaskEditorScreen
import com.changecut.feature.editor.pro.SpeedRampScreen
import com.changecut.feature.editor.pro.StickerPickerScreen
import com.changecut.feature.editor.pro.TransformScreen
import com.changecut.feature.editor.template.TemplateBrowserScreen
import com.changecut.feature.editor.settings.EditorSettingsScreen
import com.changecut.feature.editor.subtitle.SubtitleScreen
import com.changecut.feature.editor.text.TextOverlayScreen
import com.changecut.feature.editor.timeline.TimelineLayerPanel
import com.changecut.feature.editor.timeline.TimelineView
import com.changecut.feature.editor.timeline.TimelineZoomControl
import com.changecut.feature.editor.toolbar.EditToolbar
import com.changecut.core.editor.EditorClip
import com.changecut.core.editor.Track
import com.changecut.core.editor.TrackType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    projectId: String,
    onNavigateBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val subScreen by viewModel.currentSubScreen.collectAsState()
    val context = LocalContext.current
    var showExportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        viewModel.addMediaBatch(
            uris.map { uri ->
                uri.toString() to (context.contentResolver.getType(uri) ?: "video/mp4")
            }
        )
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        viewModel.addMediaBatch(
            uris.map { uri ->
                uri.toString() to (context.contentResolver.getType(uri) ?: "image/png")
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            videoPickerLauncher.launch("video/*")
        }
    }
    val imagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            imagePickerLauncher.launch("image/*")
        }
    }

    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            if (subScreen == "editor") {
                TopAppBar(
                    title = {
                        Text(
                            text = state.project?.name ?: "Editor",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.undo() },
                            enabled = state.canUndo
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, "Undo")
                        }
                        IconButton(
                            onClick = { viewModel.redo() },
                            enabled = state.canRedo
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Redo, "Redo")
                        }
                        Button(
                            onClick = { showExportDialog = true },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Export")
                        }
                        IconButton(onClick = { viewModel.navigateToSettings() }) {
                            Icon(Icons.Default.Settings, "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            } else {
                TopAppBar(
                    title = { Text(subScreenTitle(subScreen)) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.navigateToEditor() }) {
                            Icon(Icons.Default.Close, "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            if (subScreen == "editor") {
                EditorBottomNavBar(
                    currentScreen = subScreen,
                    onNavigate = { screen -> viewModel.navigateTo(screen) }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (subScreen) {
                "editor" -> EditorMainContent(
                    state = state,
                    viewModel = viewModel,
                    videoPickerLauncher = videoPickerLauncher,
                    permissionLauncher = permissionLauncher,
                    imagePickerLauncher = imagePickerLauncher,
                    imagePermissionLauncher = imagePermissionLauncher
                )
                "text" -> TextOverlayScreen(
                    trackIndex = run {
                        val selectedTrackIndex = state.selectedClipId?.let { clipId ->
                            state.tracks.indexOfFirst { track -> track.clips.any { it.id == clipId } }
                        } ?: -1
                        val selectedTrack = state.tracks.getOrNull(selectedTrackIndex)
                        if (selectedTrack?.type == com.changecut.core.editor.TrackType.TEXT) {
                            selectedTrackIndex
                        } else {
                            state.tracks.indexOfFirst { it.type == com.changecut.core.editor.TrackType.TEXT }
                                .takeIf { it >= 0 } ?: 2
                        }
                    },
                    clipId = state.selectedClipId?.takeIf { id ->
                        state.tracks.any { track ->
                            track.type == com.changecut.core.editor.TrackType.TEXT && track.clips.any { it.id == id }
                        }
                    },
                    initialText = state.selectedClipId?.let { id ->
                        state.tracks
                            .filter { it.type == com.changecut.core.editor.TrackType.TEXT }
                            .flatMap { it.clips }
                            .find { it.id == id }
                            ?.textContent
                    } ?: "",
                    initialStyle = state.selectedClipId?.let { id ->
                        state.tracks
                            .filter { it.type == com.changecut.core.editor.TrackType.TEXT }
                            .flatMap { it.clips }
                            .find { it.id == id }
                            ?.textStyle
                    },
                    onCancel = { viewModel.navigateToEditor() },
                    onApplied = { viewModel.navigateToEditor() }
                )
                "audio" -> AudioTrackScreen(
                    onBack = { viewModel.navigateToEditor() },
                    onDone = { viewModel.navigateToEditor() }
                )
                "effects" -> {
                    val clip = state.selectedClipId?.let { selectedId ->
                        state.tracks
                            .filter { it.type in setOf(TrackType.VIDEO, TrackType.OVERLAY, TrackType.STICKER, TrackType.TEXT) }
                            .flatMap { it.clips }
                            .find { it.id == selectedId }
                    }
                    if (clip == null) {
                        SelectionRequiredScreen(
                            message = "Select a visual clip to apply effects.",
                            onClose = { viewModel.navigateToEditor() }
                        )
                    } else {
                        EffectScreen(
                            clipId = clip.id,
                            onNavigateBack = { viewModel.navigateToEditor() },
                            onDone = { viewModel.navigateToEditor() }
                        )
                    }
                }
                "transition" -> {
                    val clip = state.selectedClipId?.let { clipId ->
                        state.tracks.flatMap { it.clips }.find { it.id == clipId }
                    }
                    if (clip == null) {
                        SelectionRequiredScreen(
                            message = "Select a clip to edit transitions.",
                            onClose = { viewModel.navigateToEditor() }
                        )
                    } else {
                        TransitionPickerScreen(
                            initialTransitionId = clip.transitionOut?.type,
                            initialDurationMs = clip.transitionOut?.durationMs ?: 500,
                            onNavigateBack = { viewModel.navigateToEditor() },
                            onDone = { transitionId, durationMs ->
                                viewModel.applySelectedClipTransition(
                                    transitionOut = transitionId?.let {
                                        com.changecut.core.editor.TransitionDef(
                                            id = it,
                                            type = it,
                                            durationMs = durationMs
                                        )
                                    },
                                    replaceTransitionOut = true
                                )
                                viewModel.navigateToEditor()
                            }
                        )
                    }
                }
                "transform" -> {
                    val clip = state.selectedClipId?.let { id ->
                        state.tracks.flatMap { it.clips }.find { it.id == id }
                    }
                    if (clip == null) {
                        SelectionRequiredScreen(
                            message = "Select a clip to edit transform values.",
                            onClose = { viewModel.navigateToEditor() }
                        )
                    } else {
                        TransformScreen(
                            clip = clip,
                            onApply = { positionX, positionY, scaleX, scaleY, rotation, opacity, freezeDurationMs ->
                                viewModel.updateSelectedClipTransform(
                                    positionX = positionX,
                                    positionY = positionY,
                                    scaleX = scaleX,
                                    scaleY = scaleY,
                                    rotation = rotation,
                                    opacity = opacity
                                )
                                viewModel.setSelectedClipFreeze(freezeDurationMs)
                                viewModel.navigateToEditor()
                            },
                            onClose = { viewModel.navigateToEditor() }
                        )
                    }
                }
                "keyframe" -> {
                    val clipId = state.selectedClipId
                    if (clipId == null) {
                        SelectionRequiredScreen(
                            message = "Select a clip to edit keyframes.",
                            onClose = { viewModel.navigateToEditor() }
                        )
                    } else {
                        KeyframeEditorScreen(
                            clipId = clipId,
                            existingKeyframes = viewModel.getClipKeyframes(clipId),
                            onNavigateBack = { viewModel.navigateToEditor() },
                            onDone = { items ->
                                viewModel.saveClipKeyframes(clipId, items)
                                viewModel.navigateToEditor()
                            }
                        )
                    }
                }
                "chromakey" -> {
                    val clip = state.tracks.findClipForTypes(state.selectedClipId, TrackType.VIDEO, TrackType.OVERLAY)
                    if (clip == null) {
                        SelectionRequiredScreen(
                            message = "Select a video or overlay clip to apply chroma key.",
                            onClose = { viewModel.navigateToEditor() }
                        )
                    } else {
                        ChromaKeyScreen(
                            videoPath = clip.sourceUri,
                            initialColorFilter = clip.colorFilter,
                            onNavigateBack = { viewModel.navigateToEditor() },
                            onClear = {
                                viewModel.clearChromaKeyFromSelected()
                                viewModel.navigateToEditor()
                            },
                            onApply = { colorHex, similarity, smoothness ->
                                viewModel.applyChromaKeyToSelected(colorHex, similarity, smoothness)
                                viewModel.navigateToEditor()
                            }
                        )
                    }
                }
                "speedramp" -> {
                    val clip = state.selectedClipId?.let { selectedId ->
                        state.tracks.flatMap { it.clips }.find { it.id == selectedId }
                    }
                    if (clip == null) {
                        SelectionRequiredScreen(
                            message = "Select a clip to edit speed ramp.",
                            onClose = { viewModel.navigateToEditor() }
                        )
                    } else {
                        SpeedRampScreen(
                            totalDurationUs = clip.durationUs,
                            initialPoints = viewModel.getClipSpeedRamp(clip.id),
                            onNavigateBack = { viewModel.navigateToEditor() },
                            onApply = { points ->
                                viewModel.saveClipSpeedRamp(clip.id, points)
                                viewModel.navigateToEditor()
                            }
                        )
                    }
                }
                "aiautoedit" -> AiAutoEditScreen(
                    clipPaths = state.tracks
                        .filter { it.type == com.changecut.core.editor.TrackType.VIDEO || it.type == com.changecut.core.editor.TrackType.OVERLAY }
                        .flatMap { it.clips }
                        .map { it.sourceUri }
                        .filter { it.isNotBlank() },
                    onNavigateBack = { viewModel.navigateToEditor() },
                    onDone = { viewModel.navigateToEditor() }
                )
                "aicaption" -> AiCaptionScreen(
                    videoDescription = buildCaptionContext(state),
                    onNavigateBack = { viewModel.navigateToEditor() },
                    onDone = { viewModel.navigateToEditor() },
                    onImportCaption = { caption ->
                        viewModel.addTextClip(caption, label = "AI Caption")
                    }
                )
                "subtitle" -> SubtitleScreen(
                    currentTimeUs = state.currentTimeMs * 1000L,
                    existingSubtitles = viewModel.getSubtitleItems(),
                    canAutoGenerate = state.selectedClipId != null || state.tracks.any { track ->
                        track.clips.any { clip -> clip.sourceUri.isNotBlank() }
                    },
                    onAutoGenerate = { viewModel.generateSubtitleItemsFromSelection() },
                    onBack = { viewModel.navigateToEditor() },
                    onDone = { subtitles ->
                        viewModel.addSubtitleClips(subtitles)
                        viewModel.navigateToEditor()
                    }
                )
                "aivoiceover" -> AiVoiceOverScreen(
                    onNavigateBack = { viewModel.navigateToEditor() },
                    onDone = { viewModel.navigateToEditor() },
                    onImportToTimeline = { path ->
                        viewModel.addAudioFile(path, "AI Voice Over")
                    }
                )
                "mask" -> {
                    val clip = state.tracks.findClipForTypes(state.selectedClipId, TrackType.VIDEO, TrackType.OVERLAY, TrackType.STICKER, TrackType.TEXT)
                    if (clip == null) {
                        SelectionRequiredScreen(
                            message = "Select a clip to edit mask and blend.",
                            onClose = { viewModel.navigateToEditor() }
                        )
                    } else {
                        MaskEditorScreen(
                            initialMask = clip.mask,
                            initialBlendMode = clip.blendMode,
                            onApply = { mask, blend ->
                                viewModel.applyMaskAndBlendToSelected(mask, blend)
                                viewModel.navigateToEditor()
                            },
                            onClose = { viewModel.navigateToEditor() }
                        )
                    }
                }
                "sticker" -> StickerPickerScreen(
                    onApply = { sticker ->
                        val selectedClipId = state.selectedClipId
                        if (selectedClipId != null) {
                            viewModel.applyStickerToSelected(sticker)
                        } else if (sticker != null) {
                            viewModel.addStickerClip(sticker)
                        }
                        viewModel.navigateToEditor()
                    },
                    onClose = { viewModel.navigateToEditor() }
                )
                "animation" -> {
                    val clip = state.tracks.findClipForTypes(state.selectedClipId, TrackType.VIDEO, TrackType.OVERLAY, TrackType.STICKER, TrackType.TEXT)
                    if (clip == null) {
                        SelectionRequiredScreen(
                            message = "Select a clip to edit animations.",
                            onClose = { viewModel.navigateToEditor() }
                        )
                    } else {
                        AnimationPickerScreen(
                            currentIn = clip.animationIn,
                            currentOut = clip.animationOut,
                            onApply = { animIn, animOut ->
                                viewModel.applyAnimationToSelected(animIn, animOut)
                                viewModel.navigateToEditor()
                            },
                            onClose = { viewModel.navigateToEditor() }
                        )
                    }
                }
                "colorgrading" -> {
                    val clip = state.tracks.findClipForTypes(state.selectedClipId, TrackType.VIDEO, TrackType.OVERLAY, TrackType.STICKER)
                    if (clip == null) {
                        SelectionRequiredScreen(
                            message = "Select a visual clip to edit color grading.",
                            onClose = { viewModel.navigateToEditor() }
                        )
                    } else {
                        ColorGradingScreen(
                            currentGrade = clip.colorGrade,
                            onApply = { grade ->
                                viewModel.applyColorGradeToSelected(grade)
                                viewModel.navigateToEditor()
                            },
                            onClose = { viewModel.navigateToEditor() }
                        )
                    }
                }
                "audioeq" -> {
                    val clip = state.tracks.findClipForTypes(state.selectedClipId, TrackType.VIDEO, TrackType.AUDIO)
                    if (clip == null) {
                        SelectionRequiredScreen(
                            message = "Select a video or audio clip to edit audio EQ.",
                            onClose = { viewModel.navigateToEditor() }
                        )
                    } else {
                        AudioEQScreen(
                            currentEQ = clip.audioEQ,
                            onApply = { eq ->
                                viewModel.applyAudioEqToSelected(eq)
                                viewModel.navigateToEditor()
                            },
                            onClose = { viewModel.navigateToEditor() }
                        )
                    }
                }
                "adjustment" -> AdjustmentLayerScreen(
                    tracks = state.tracks,
                    groups = state.groups,
                    selectedClipCount = state.selectedClipIds.size + if (state.selectedClipIds.isEmpty() && state.selectedClipId != null) 1 else 0,
                    onAddGroup = { viewModel.createGroupFromSelection() },
                    onAddAdjustmentTrack = { viewModel.addAdjustmentTrack() },
                    onClearSelection = { viewModel.clearClipSelection() },
                    onSelectGroup = { viewModel.selectGroup(it) },
                    onDeleteGroup = { viewModel.deleteGroup(it) },
                    onToggleGroupExpand = { viewModel.toggleGroupExpand(it) },
                    onClose = { viewModel.navigateToEditor() }
                )
                "templates" -> TemplateBrowserScreen(
                    engine = viewModel.templateEngine,
                    onApplyTemplate = { template ->
                        viewModel.applyTemplate(template)
                    },
                    onSaveCurrentTemplate = { name ->
                        viewModel.saveCurrentTimelineAsTemplate(name)
                    },
                    onClose = { viewModel.navigateToEditor() }
                )
                "settings" -> EditorSettingsScreen(
                    onNavigateBack = { viewModel.navigateToEditor() }
                )
            }
        }
    }

    if (state.exportOutputPath != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearExportOutput() },
            title = { Text("Export Complete") },
            text = { Text("Video saved to: ${state.exportOutputPath}") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearExportOutput()
                    onNavigateBack()
                }) { Text("Done") }
            }
        )
    }

    if (showExportDialog) {
        ExportSettingsDialog(
            onDismiss = { showExportDialog = false },
            onExport = { width, height, fps ->
                showExportDialog = false
                viewModel.exportVideo(width = width, height = height, fps = fps)
            }
        )
    }
}

@Composable
private fun EditorMainContent(
    state: EditorUiState,
    viewModel: EditorViewModel,
    videoPickerLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    imagePickerLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    imagePermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        GpuPreviewView(
            renderer = viewModel.glRenderer,
            scheduler = viewModel.frameScheduler,
            tracks = state.tracks,
            currentTimeUs = state.currentTimeMs * 1000L,
            isPlaying = state.isPlaying,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        TimelineZoomControl(
            zoomLevel = state.zoomLevel,
            onZoomChange = { viewModel.setZoom(it) },
            modifier = Modifier.fillMaxWidth()
        )

        Row(modifier = Modifier.fillMaxWidth().weight(1.5f)) {
            TimelineLayerPanel(
                tracks = state.tracks,
                selectedTrackIndex = state.selectedTrackIndex,
                selectedClipIds = state.selectedClipIds.ifEmpty {
                    state.selectedClipId?.let { setOf(it) } ?: emptySet()
                },
                onTrackSelected = { index ->
                    viewModel.selectTrack(index)
                },
                onToggleVisibility = { viewModel.toggleTrackVisibility(it) },
                onToggleMute = { viewModel.toggleTrackMute(it) },
                onToggleLock = { viewModel.toggleTrackLock(it) },
                onSoloTrack = { viewModel.soloTrack(it) },
                onShowAllTracks = { viewModel.showAllTracks() },
                onTrackSelectAll = { viewModel.selectAllOnTrack(it) }
            )

            TimelineView(
                tracks = state.tracks,
                currentTimeMs = state.currentTimeMs,
                durationMs = state.durationMs,
                selectedTrackIndex = state.selectedTrackIndex,
                selectedClipId = state.selectedClipId,
                selectedClipIds = state.selectedClipIds,
                snapEnabled = state.snapEnabled,
                zoomLevel = state.zoomLevel,
                onTimeChange = { viewModel.setCurrentTime(it) },
                onClipSelected = { clipId, trackIndex ->
                    viewModel.selectClip(clipId, trackIndex)
                },
                onClipMultiToggle = { clipId, trackIndex ->
                    viewModel.toggleClipMultiSelect(clipId, trackIndex)
                },
                onTrackSelected = { index ->
                    viewModel.selectTrack(index)
                },
                onPlayPause = { viewModel.togglePlay() },
                isPlaying = state.isPlaying,
                modifier = Modifier.weight(1f)
            )
        }

        EditToolbar(
            selectedClipId = state.selectedClipId,
            hasSelection = state.selectedClipId != null || state.selectedClipIds.isNotEmpty(),
            selectedClipCount = state.selectedClipIds.size.takeIf { it > 0 } ?: if (state.selectedClipId != null) 1 else 0,
            snapEnabled = state.snapEnabled,
            onAddVideo = {
                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_VIDEO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                    videoPickerLauncher.launch("video/*")
                } else {
                    permissionLauncher.launch(permission)
                }
            },
            onAddImage = {
                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                    imagePickerLauncher.launch("image/*")
                } else {
                    imagePermissionLauncher.launch(permission)
                }
            },
            onAddAudio = { viewModel.navigateToAudio() },
            onAddText = { viewModel.navigateToTextOverlay() },
            onTemplates = { viewModel.navigateToTemplates() },
            onSticker = { viewModel.navigateToSticker() },
            onMask = { viewModel.navigateToMask() },
            onChromaKey = { viewModel.navigateToChromaKey() },
            onColor = { viewModel.navigateToColorGrading() },
            onSpeedRamp = { viewModel.navigateToSpeedRamp() },
            onEffects = { viewModel.navigateToEffects() },
            onTransition = { viewModel.navigateToTransition() },
            onKeyframe = { viewModel.navigateToKeyframe() },
            onAiAutoEdit = { viewModel.navigateToAiAutoEdit() },
            onAiVoiceOver = { viewModel.navigateToAiVoiceOver() },
            onAiCaption = { viewModel.navigateToAiCaption() },
            onSubtitle = { viewModel.navigateToSubtitle() },
            onAnimation = { viewModel.navigateToAnimation() },
            onAudioEq = { viewModel.navigateToAudioEQ() },
            onAdjustment = { viewModel.navigateToAdjustment() },
            onSelectTrack = { viewModel.selectAllOnCurrentTrack() },
            onToggleSnap = { viewModel.toggleSnap() },
            onCopyStyle = { viewModel.copySelectedClipStyle() },
            onPasteStyle = { viewModel.pasteStyleToSelection() },
            onDuplicate = { viewModel.duplicateSelection() },
            onNudgeLeft = { viewModel.nudgeSelectedClip(-100L) },
            onNudgeRight = { viewModel.nudgeSelectedClip(100L) },
            onSplit = {
                state.selectedClipId?.let { clipId ->
                    viewModel.splitClip(clipId, state.currentTimeMs)
                }
            },
            onMerge = { viewModel.mergeSelection() },
            onDelete = { viewModel.deleteSelectedClip() },
            onRippleDelete = { viewModel.rippleDeleteSelectedClip() }
        )

        if (state.isExporting) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.width(24.dp))
                LinearProgressIndicator(
                    progress = { state.exportProgress },
                    modifier = Modifier.weight(1f)
                )
                Text("Exporting...")
            }
        }
    }
}

@Composable
private fun EditorBottomNavBar(
    currentScreen: String,
    onNavigate: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavBarItem(Icons.Default.Edit, "Edit", currentScreen == "editor") { onNavigate("editor") }
            NavBarItem(Icons.Default.Layers, "Mask", currentScreen == "mask") { onNavigate("mask") }
            NavBarItem(Icons.Default.TextFields, "Text", currentScreen == "text") { onNavigate("text") }
            NavBarItem(Icons.Default.MusicNote, "Audio", currentScreen == "audio") { onNavigate("audio") }
            NavBarItem(Icons.Default.ColorLens, "Effects", currentScreen == "effects") { onNavigate("effects") }
            NavBarItem(Icons.Default.AutoAwesome, "Trans", currentScreen == "transition") { onNavigate("transition") }
            NavBarItem(Icons.Default.Edit, "Transform", currentScreen == "transform") { onNavigate("transform") }
            NavBarItem(Icons.Default.Tune, "Keyframe", currentScreen == "keyframe") { onNavigate("keyframe") }
            NavBarItem(Icons.Default.AutoAwesome, "AI", currentScreen == "aiautoedit") { onNavigate("aiautoedit") }
            NavBarItem(Icons.Default.Settings, "Settings", currentScreen == "settings") { onNavigate("settings") }
        }
    }
}

@Composable
private fun NavBarItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = tint
        )
        Text(
            text = label,
            fontSize = 9.sp,
            color = tint,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun subScreenTitle(screen: String): String = when (screen) {
    "text" -> "Text Overlay"
    "audio" -> "Audio Track"
    "effects" -> "Effects"
    "transition" -> "Transitions"
    "transform" -> "Transform"
    "keyframe" -> "Keyframe Editor"
    "chromakey" -> "Chroma Key"
    "mask" -> "Mask & Blend"
    "sticker" -> "Stickers"
    "animation" -> "Animations"
    "colorgrading" -> "Color Grading"
    "audioeq" -> "Audio EQ"
    "adjustment" -> "Adjustment & Groups"
    "templates" -> "Templates"
    "speedramp" -> "Speed Ramp"
    "aiautoedit" -> "AI Auto Edit"
    "aicaption" -> "AI Caption"
    "subtitle" -> "Subtitles"
    "aivoiceover" -> "AI Voice Over"
    "settings" -> "Editor Settings"
    else -> "Editor"
}

private fun buildCaptionContext(state: EditorUiState): String {
    val selectedClip = state.selectedClipId?.let { selectedId ->
        state.tracks.flatMap { it.clips }.find { it.id == selectedId }
    }
    if (selectedClip != null) {
        val selectedTrack = state.tracks.firstOrNull { track -> track.clips.any { it.id == selectedClip.id } }
        return listOfNotNull(
            state.project?.name?.takeIf { it.isNotBlank() },
            selectedTrack?.type?.name?.lowercase(),
            selectedClip.contentRole,
            selectedClip.label.takeIf { it.isNotBlank() },
            selectedClip.textContent?.takeIf { it.isNotBlank() }
        ).joinToString(" | ")
    }

    val visualLabels = state.tracks
        .filter { it.type in setOf(TrackType.VIDEO, TrackType.OVERLAY, TrackType.STICKER, TrackType.TEXT) }
        .flatMap { it.clips }
        .sortedBy { it.startOffsetUs }
        .mapNotNull { clip ->
            clip.textContent?.takeIf { it.isNotBlank() }
                ?: clip.label.takeIf { it.isNotBlank() }
        }
        .distinct()
        .take(6)

    return listOfNotNull(
        state.project?.name?.takeIf { it.isNotBlank() },
        visualLabels.takeIf { it.isNotEmpty() }?.joinToString(", ")
    ).joinToString(" | ")
}

private fun List<Track>.findClipForTypes(
    selectedClipId: String?,
    vararg allowedTypes: TrackType
): EditorClip? {
    val allowed = allowedTypes.toSet()
    return firstOrNull { track ->
        track.type in allowed && track.clips.any { it.id == selectedClipId }
    }?.clips?.find { it.id == selectedClipId }
}

@Composable
private fun SelectionRequiredScreen(
    message: String,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onClose) {
                Text("Back To Editor")
            }
        }
    }
}

@Composable
private fun ExportSettingsDialog(
    onDismiss: () -> Unit,
    onExport: (Int, Int, Int) -> Unit
) {
    var widthText by remember { mutableStateOf("1080") }
    var heightText by remember { mutableStateOf("1920") }
    var fpsText by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = widthText,
                    onValueChange = { widthText = it.filter(Char::isDigit) },
                    label = { Text("Width") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it.filter(Char::isDigit) },
                    label = { Text("Height") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fpsText,
                    onValueChange = { fpsText = it.filter(Char::isDigit) },
                    label = { Text("FPS") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val width = widthText.toIntOrNull()?.coerceAtLeast(240) ?: 1080
                    val height = heightText.toIntOrNull()?.coerceAtLeast(240) ?: 1920
                    val fps = fpsText.toIntOrNull()?.coerceIn(12, 120) ?: 30
                    onExport(width, height, fps)
                }
            ) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
