package com.changecut.feature.editor

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.changecut.core.editor.ClipGroup
import com.changecut.core.editor.EditorClip
import com.changecut.core.editor.SnapshotCommand
import com.changecut.core.editor.SplitClipCommand
import com.changecut.core.editor.Track
import com.changecut.core.editor.TrackManager
import com.changecut.core.editor.TrackType
import com.changecut.core.editor.TransitionDef
import com.changecut.core.editor.UndoRedoManager
import com.changecut.core.editor.StickerDef
import com.changecut.core.editor.TemplateProject
import com.changecut.core.editor.TextClipStyle
import com.changecut.feature.editor.keyframe.KeyframeItem
import com.changecut.feature.editor.pro.SpeedControlPoint
import com.changecut.feature.editor.subtitle.SubtitleItem
import com.changecut.core.export.HardwareExporter
import com.changecut.core.ffmpeg.VideoEngine
import com.changecut.core.gpu.FrameScheduler
import com.changecut.core.gpu.GlVideoRenderer
import com.changecut.core.media.MediaProjectManager
import com.changecut.core.media.MediaUtils
import com.changecut.core.media.ProjectTimelineStore
import com.changecut.core.ml.PortraitSegmentation
import com.changecut.core.ml.SpeechTranscriptEngine
import com.changecut.core.template.TemplateEngine
import com.changecut.domain.model.Project
import com.changecut.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class EditorUiState(
    val project: Project? = null,
    val tracks: List<Track> = emptyList(),
    val groups: List<ClipGroup> = emptyList(),
    val currentTimeMs: Long = 0L,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val selectedClipId: String? = null,
    val selectedClipIds: Set<String> = emptySet(),
    val selectedTrackIndex: Int = 0,
    val snapEnabled: Boolean = true,
    val zoomLevel: Float = 1f,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val exportOutputPath: String? = null,
    val error: String? = null,
    val currentSubScreen: String = "editor"
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val videoEngine: VideoEngine,
    private val mediaUtils: MediaUtils,
    private val mediaProjectManager: MediaProjectManager,
    private val projectTimelineStore: ProjectTimelineStore,
    val trackManager: TrackManager,
    val undoRedoManager: UndoRedoManager,
    val templateEngine: TemplateEngine,
    val glRenderer: GlVideoRenderer,
    val frameScheduler: FrameScheduler,
    private val hardwareExporter: HardwareExporter,
    private val portraitSegmentation: PortraitSegmentation,
    val speechTranscriptEngine: SpeechTranscriptEngine
) : ViewModel() {
    private var isRestoringState: Boolean = false
    private data class TimelineUiPersistence(
        val tracks: List<Track>,
        val groups: List<ClipGroup>,
        val zoomLevel: Float,
        val snapEnabled: Boolean
    )
    private data class ClipStyleClipboard(
        val sourceTrackType: TrackType?,
        val contentRole: String?,
        val volume: Float,
        val audioFadeInMs: Int,
        val audioFadeOutMs: Int,
        val speed: Float,
        val rotation: Float,
        val scaleX: Float,
        val scaleY: Float,
        val positionX: Float,
        val positionY: Float,
        val opacity: Float,
        val textStyle: TextClipStyle?,
        val effectId: String?,
        val transitionIn: TransitionDef?,
        val transitionOut: TransitionDef?,
        val colorFilter: String?,
        val mask: com.changecut.core.editor.MaskDef?,
        val blendMode: com.changecut.core.editor.BlendMode,
        val sticker: StickerDef?,
        val animationIn: com.changecut.core.editor.AnimationDef?,
        val animationOut: com.changecut.core.editor.AnimationDef?,
        val colorGrade: com.changecut.core.editor.ColorGradeDef?,
        val audioEQ: com.changecut.core.editor.AudioEQDef?,
        val freezeDurationMs: Int
    )
    private var styleClipboard: ClipStyleClipboard? = null

    init {
        viewModelScope.launch {
            frameScheduler.currentRenderTimeUs.collect { timeUs ->
                trackManager.seekTo(timeUs)
            }
        }
        viewModelScope.launch {
            combine(
                trackManager.tracks,
                trackManager.groups,
                trackManager.zoomLevel,
                trackManager.snapEnabled
            ) { tracks, groups, zoomLevel, snapEnabled ->
                TimelineUiPersistence(tracks, groups, zoomLevel, snapEnabled)
            }.collect { persistence ->
                val project = _project.value ?: return@collect
                if (isRestoringState) return@collect
                projectTimelineStore.save(
                    projectId = project.id,
                    tracks = persistence.tracks,
                    groups = persistence.groups,
                    zoomLevel = persistence.zoomLevel,
                    snapEnabled = persistence.snapEnabled
                )
                val updatedProject = project.copy(
                    durationMs = (trackManager.totalDurationUs.value / 1000L),
                    updatedAt = System.currentTimeMillis()
                )
                projectRepository.saveProject(updatedProject)
                _project.value = updatedProject
            }
        }
    }

    private val _isPlaying = MutableStateFlow(false)
    private val _isExporting = MutableStateFlow(false)
    private val _exportProgress = MutableStateFlow(0f)
    private val _exportOutputPath = MutableStateFlow<String?>(null)
    private val _error = MutableStateFlow<String?>(null)
    private val _project = MutableStateFlow<Project?>(null)

    private val _currentSubScreen = MutableStateFlow("editor")
    val currentSubScreen: StateFlow<String> = _currentSubScreen.asStateFlow()

    val state: StateFlow<EditorUiState> = combine(
        trackManager.tracks,
        trackManager.groups,
        trackManager.currentTimeUs,
        trackManager.totalDurationUs,
        trackManager.selectedClipId,
        trackManager.selectedClipIds,
        trackManager.selectedTrackIndex,
        trackManager.snapEnabled,
        trackManager.zoomLevel,
        undoRedoManager.canUndo,
        undoRedoManager.canRedo,
        _isPlaying,
        _project,
        _isExporting,
        _exportProgress,
        _exportOutputPath,
        _error,
        _currentSubScreen
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        EditorUiState(
            project = args[12] as? Project,
            tracks = args[0] as List<Track>,
            groups = args[1] as List<ClipGroup>,
            currentTimeMs = (args[2] as Long) / 1000L,
            isPlaying = args[11] as Boolean,
            durationMs = (args[3] as Long) / 1000L,
            selectedClipId = args[4] as? String,
            selectedClipIds = args[5] as Set<String>,
            selectedTrackIndex = args[6] as Int,
            snapEnabled = args[7] as Boolean,
            zoomLevel = args[8] as Float,
            canUndo = args[9] as Boolean,
            canRedo = args[10] as Boolean,
            isExporting = args[13] as Boolean,
            exportProgress = args[14] as Float,
            exportOutputPath = args[15] as? String,
            error = args[16] as? String,
            currentSubScreen = args[17] as String
        )
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), EditorUiState())

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            val project = projectRepository.getProjectById(projectId)
            if (project != null) {
                _project.value = project
                val savedState = projectTimelineStore.load(project.id)
                isRestoringState = true
                undoRedoManager.clear()
                _isPlaying.value = false
                frameScheduler.pause()
                frameScheduler.seekTo(0L)
                if (savedState != null) {
                    trackManager.replaceState(
                        tracks = savedState.tracks,
                        groups = savedState.groups,
                        zoomLevel = savedState.zoomLevel,
                        snapEnabled = savedState.snapEnabled
                    )
                } else {
                    trackManager.clear()
                }
                isRestoringState = false
            }
        }
    }

    fun addMedia(uriString: String, mimeType: String) {
        viewModelScope.launch {
            importMediaInternal(Uri.parse(uriString), mimeType)
        }
    }

    fun addMediaBatch(items: List<Pair<String, String>>) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            undoRedoManager.execute(
                SnapshotCommand(trackManager, "Import media batch") {
                    items.forEach { (uriString, mimeType) ->
                        importMediaInternal(Uri.parse(uriString), mimeType, wrapInHistory = false)
                    }
                }
            )
        }
    }

    fun addAudioFile(path: String, label: String = File(path).nameWithoutExtension.ifBlank { "Audio" }) {
        viewModelScope.launch {
            val projectId = _project.value?.id
            val sourceFile = File(path)
            val resolvedPath = if (projectId != null && sourceFile.exists()) {
                val mediaDir = mediaProjectManager.getMediaDir(projectId)
                val targetFile = File(mediaDir, sourceFile.name)
                if (sourceFile.absolutePath != targetFile.absolutePath) {
                    sourceFile.copyTo(targetFile, overwrite = true)
                }
                targetFile.absolutePath
            } else {
                path
            }
            val durationUs = ((videoEngine.getMediaInfo(resolvedPath)?.durationMs ?: 0L) * 1000L).coerceAtLeast(1_000_000L)
            val clip = EditorClip(
                id = UUID.randomUUID().toString(),
                sourceUri = resolvedPath,
                label = label,
                startOffsetUs = 0L,
                endOffsetUs = durationUs
            )
            undoRedoManager.execute(
                SnapshotCommand(trackManager, "Add audio clip") {
                    var trackIndex = trackManager.tracks.value.indexOfFirst { it.type == TrackType.AUDIO }
                    if (trackIndex < 0) {
                        trackManager.addTrack(TrackType.AUDIO, "Audio")
                        trackIndex = trackManager.tracks.value.indexOfFirst { it.type == TrackType.AUDIO }
                    }
                    if (trackIndex < 0) return@SnapshotCommand
                    val audioTrack = trackManager.tracks.value[trackIndex]
                    val timelineStartUs = audioTrack.clips.maxOfOrNull { it.endOffsetUs } ?: 0L
                    trackManager.addClip(
                        trackIndex,
                        clip.copy(
                            startOffsetUs = timelineStartUs,
                            endOffsetUs = timelineStartUs + durationUs
                        )
                    )
                    trackManager.selectTrack(trackIndex)
                    trackManager.selectClip(clip.id)
                }
            )
        }
    }

    fun addTextClip(
        text: String,
        durationMs: Long = 3_000L,
        label: String = "Text"
    ) {
        if (text.isBlank()) return
        val durationUs = durationMs.coerceAtLeast(500L) * 1000L
        val clip = EditorClip(
            id = UUID.randomUUID().toString(),
            sourceUri = "",
            label = label,
            contentRole = if (label.equals("AI Caption", ignoreCase = true)) "caption" else "text",
            startOffsetUs = 0L,
            endOffsetUs = durationUs,
            textContent = text,
            textStyle = defaultAutoTextStyle(position = "caption"),
            positionX = 0.5f,
            positionY = 0.78f
        )
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Add text clip") {
                var trackIndex = trackManager.tracks.value.indexOfFirst { it.type == TrackType.TEXT }
                if (trackIndex < 0) {
                    trackManager.addTrack(TrackType.TEXT, "Text")
                    trackIndex = trackManager.tracks.value.indexOfFirst { it.type == TrackType.TEXT }
                }
                if (trackIndex < 0) return@SnapshotCommand
                val currentTimeUs = trackManager.currentTimeUs.value
                trackManager.addClip(
                    trackIndex,
                    clip.copy(
                        startOffsetUs = currentTimeUs,
                        endOffsetUs = currentTimeUs + durationUs
                    )
                )
                trackManager.selectTrack(trackIndex)
                trackManager.selectClip(clip.id)
            }
        )
    }

    fun addSubtitleClips(items: List<SubtitleItem>) {
        if (items.isEmpty()) return
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Add subtitle clips") {
                removeExistingSubtitleClips()
                var trackIndex = trackManager.tracks.value.indexOfFirst { it.type == TrackType.TEXT }
                if (trackIndex < 0) {
                    trackManager.addTrack(TrackType.TEXT, "Text")
                    trackIndex = trackManager.tracks.value.indexOfFirst { it.type == TrackType.TEXT }
                }
                if (trackIndex < 0) return@SnapshotCommand

                items.sortedBy { it.startUs }.forEach { item ->
                    if (item.text.isBlank() || item.endUs <= item.startUs) return@forEach
                    trackManager.addClip(
                        trackIndex,
                        EditorClip(
                            id = UUID.randomUUID().toString(),
                            sourceUri = "",
                            label = "Subtitle: ${item.text.take(20)}",
                            contentRole = "subtitle",
                            startOffsetUs = item.startUs,
                            endOffsetUs = item.endUs,
                            textContent = item.text,
                            textStyle = defaultAutoTextStyle(position = "subtitle"),
                            positionX = 0.5f,
                            positionY = 0.86f
                        )
                    )
                }
                trackManager.selectTrack(trackIndex)
            }
        )
    }

    fun getSubtitleItems(): List<SubtitleItem> {
        return trackManager.tracks.value
            .filter { it.type == TrackType.TEXT }
            .flatMap { it.clips }
            .filter { isSubtitleClip(it) }
            .sortedBy { it.startOffsetUs }
            .map { clip ->
                SubtitleItem(
                    id = clip.id,
                    startUs = clip.startOffsetUs,
                    endUs = clip.endOffsetUs,
                    text = clip.textContent.orEmpty()
                )
            }
    }

    fun generateSubtitleItemsFromSelection(): List<SubtitleItem> {
        val sourceClip = trackManager.selectedClipId.value
            ?.let(trackManager::getClip)
            ?.takeIf { it.sourceUri.isNotBlank() }
            ?: trackManager.tracks.value
                .flatMap { it.clips }
                .firstOrNull { it.sourceUri.isNotBlank() && (it.endOffsetUs > it.startOffsetUs) }
            ?: return emptyList()

        speechTranscriptEngine.transcribe(sourceClip.sourceUri)
        val trimStartMs = (sourceClip.trimStartUs / 1000L).coerceAtLeast(0L)
        val trimEndMs = if (sourceClip.trimEndUs > sourceClip.trimStartUs) {
            sourceClip.trimEndUs / 1000L
        } else {
            Long.MAX_VALUE
        }
        val clipSpeed = sourceClip.speed.coerceAtLeast(0.01f)

        return speechTranscriptEngine.segments.value.mapNotNull { segment ->
            val overlapStartMs = maxOf(segment.startMs, trimStartMs)
            val overlapEndMs = minOf(segment.endMs, trimEndMs)
            if (overlapEndMs <= overlapStartMs) {
                return@mapNotNull null
            }
            val timelineStartUs = sourceClip.startOffsetUs + (((overlapStartMs - trimStartMs) * 1000f) / clipSpeed).toLong()
            val timelineEndUs = sourceClip.startOffsetUs + (((overlapEndMs - trimStartMs) * 1000f) / clipSpeed).toLong()
            SubtitleItem(
                id = UUID.randomUUID().toString(),
                startUs = timelineStartUs,
                endUs = timelineEndUs,
                text = segment.text.ifBlank { "[speech]" }
            )
        }
    }

    fun setCurrentTime(timeMs: Long) {
        val timeUs = timeMs * 1000L
        trackManager.seekTo(timeUs)
        frameScheduler.seekTo(timeUs)
    }

    fun togglePlay() {
        val playing = !_isPlaying.value
        _isPlaying.value = playing
        if (playing) {
            frameScheduler.setDuration(trackManager.totalDurationUs.value)
            frameScheduler.seekTo(trackManager.currentTimeUs.value)
            frameScheduler.play()
        } else {
            frameScheduler.pause()
        }
    }

    fun selectClip(clipId: String?, trackIndex: Int) {
        trackManager.selectClip(clipId)
        if (trackIndex >= 0) trackManager.selectTrack(trackIndex)
    }

    fun selectTrack(index: Int) {
        trackManager.selectTrack(index)
    }

    fun toggleClipMultiSelect(clipId: String, trackIndex: Int) {
        trackManager.toggleMultiSelect(clipId)
        if (trackIndex >= 0) trackManager.selectTrack(trackIndex)
    }

    fun trimClip(clipId: String, newStartMs: Long, newEndMs: Long) {
        val tracks = trackManager.tracks.value
        for ((index, track) in tracks.withIndex()) {
            if (track.clips.any { it.id == clipId }) {
                undoRedoManager.execute(
                    SnapshotCommand(trackManager, "Trim clip") {
                        trackManager.trimClipBounds(index, clipId, newStartMs * 1000L, newEndMs * 1000L)
                    }
                )
                return
            }
        }
    }

    fun moveClipToTime(clipId: String, newStartMs: Long) {
        val tracks = trackManager.tracks.value
        for ((index, track) in tracks.withIndex()) {
            if (track.clips.any { it.id == clipId }) {
                undoRedoManager.execute(
                    SnapshotCommand(trackManager, "Move clip in time") {
                        trackManager.moveClipInTime(index, clipId, newStartMs * 1000L)
                    }
                )
                return
            }
        }
    }

    fun nudgeSelectedClip(deltaMs: Long) {
        val selectedIds = trackManager.selectedClipIds.value
        if (selectedIds.size > 1) {
            undoRedoManager.execute(
                SnapshotCommand(trackManager, "Nudge selected clips") {
                    trackManager.moveSelectedClipsInTime(deltaMs * 1000L)
                }
            )
            return
        }
        val clipId = trackManager.selectedClipId.value ?: return
        val clip = trackManager.getClip(clipId) ?: return
        moveClipToTime(clipId, ((clip.startOffsetUs / 1000L) + deltaMs).coerceAtLeast(0L))
    }

    fun splitClip(clipId: String, splitTimeMs: Long) {
        val tracks = trackManager.tracks.value
        for ((index, track) in tracks.withIndex()) {
            if (track.clips.any { it.id == clipId }) {
                val command = SplitClipCommand(trackManager, index, clipId, splitTimeMs * 1000L)
                undoRedoManager.execute(command)
                return
            }
        }
    }

    fun mergeSelection() {
        val selectedIds = trackManager.selectedClipIds.value.ifEmpty {
            trackManager.selectedClipId.value?.let { setOf(it) } ?: emptySet()
        }
        if (selectedIds.size < 2) {
            _error.value = "Select at least two clips to merge"
            return
        }
        val ownerTrackCount = trackManager.tracks.value.count { track ->
            track.clips.any { it.id in selectedIds }
        }
        if (ownerTrackCount != 1) {
            _error.value = "Merge only works for clips on the same track"
            return
        }
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Merge selected clips") {
                trackManager.mergeSelectedClips()
            }
        )
        val afterIds = trackManager.selectedClipIds.value
        if (afterIds.size != 1) {
            undoRedoManager.undo()
            _error.value = "Merge requires adjacent clips on the same track"
        }
    }

    fun reorderClip(fromIndex: Int, toIndex: Int) {
        val tracks = trackManager.tracks.value
        val track = tracks.getOrNull(trackManager.selectedTrackIndex.value) ?: return
        val clips = track.clips
        if (fromIndex !in clips.indices || toIndex !in clips.indices) return
        val clipId = clips[fromIndex].id
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Reorder clip") {
                trackManager.moveClip(trackManager.selectedTrackIndex.value, clipId, toIndex)
            }
        )
    }

    fun removeClip(clipId: String) {
        val tracks = trackManager.tracks.value
        for ((index, track) in tracks.withIndex()) {
            val clip = track.clips.find { it.id == clipId }
            if (clip != null) {
                undoRedoManager.execute(
                    SnapshotCommand(trackManager, "Remove clip ${clip.label}") {
                        trackManager.removeClip(index, clipId)
                        trackManager.selectClip(null)
                    }
                )
                return
            }
        }
    }

    fun deleteSelectedClip() {
        val selectedIds = trackManager.selectedClipIds.value
        if (selectedIds.isNotEmpty()) {
            undoRedoManager.execute(
                SnapshotCommand(trackManager, "Delete selected clips") {
                    trackManager.deleteSelected()
                }
            )
            return
        }
        val clipId = trackManager.selectedClipId.value ?: return
        removeClip(clipId)
    }

    fun rippleDeleteSelectedClip() {
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Ripple delete selection") {
                trackManager.rippleDeleteSelection()
            }
        )
    }

    fun duplicateSelection() {
        val selectedIds = trackManager.selectedClipIds.value.ifEmpty {
            trackManager.selectedClipId.value?.let { setOf(it) } ?: emptySet()
        }
        if (selectedIds.isEmpty()) return
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Duplicate selection") {
                val selectedClips = trackManager.getAllClips()
                    .filter { it.id in selectedIds }
                    .sortedBy { it.startOffsetUs }
                if (selectedClips.isEmpty()) return@SnapshotCommand

                val rangeStartUs = selectedClips.minOf { it.startOffsetUs }
                val rangeEndUs = selectedClips.maxOf { it.endOffsetUs }
                val duplicateOffsetUs = (rangeEndUs - rangeStartUs).coerceAtLeast(0L) + 250_000L
                val newIds = mutableSetOf<String>()
                val idRemap = linkedMapOf<String, String>()
                var primaryNewId: String? = null

                trackManager.tracks.value.forEachIndexed { trackIndex, track ->
                    val duplicates = track.clips
                        .filter { it.id in selectedIds }
                        .sortedBy { it.startOffsetUs }
                        .map { clip ->
                            val newId = UUID.randomUUID().toString()
                            idRemap[clip.id] = newId
                            if (clip.id == trackManager.selectedClipId.value) {
                                primaryNewId = newId
                            }
                            newIds += newId
                            duplicateClipForTimeline(clip).copy(
                                id = newId,
                                label = buildDuplicatedLabel(clip.label),
                                startOffsetUs = clip.startOffsetUs + duplicateOffsetUs,
                                endOffsetUs = clip.endOffsetUs + duplicateOffsetUs
                            )
                        }
                    duplicates.forEach { duplicate ->
                        trackManager.addClip(trackIndex, duplicate)
                    }
                }

                trackManager.groups.value
                    .filter { group -> group.clipIds.isNotEmpty() && group.clipIds.all { it in selectedIds } }
                    .forEach { group ->
                        val duplicatedGroupClipIds = group.clipIds.mapNotNull(idRemap::get)
                        if (duplicatedGroupClipIds.isNotEmpty()) {
                            trackManager.createGroup("${group.name} Copy", duplicatedGroupClipIds)
                        }
                    }

                if (newIds.isNotEmpty()) {
                    trackManager.setSelection(primaryNewId ?: newIds.first(), newIds)
                }
            }
        )
    }

    fun copySelectedClipStyle() {
        val clipId = trackManager.selectedClipId.value ?: return
        val clip = trackManager.getClip(clipId) ?: return
        val sourceTrackType = trackManager.tracks.value
            .firstOrNull { track -> track.clips.any { it.id == clipId } }
            ?.type
        styleClipboard = ClipStyleClipboard(
            sourceTrackType = sourceTrackType,
            contentRole = clip.contentRole,
            volume = clip.volume,
            audioFadeInMs = clip.audioFadeInMs,
            audioFadeOutMs = clip.audioFadeOutMs,
            speed = clip.speed,
            rotation = clip.rotation,
            scaleX = clip.scaleX,
            scaleY = clip.scaleY,
            positionX = clip.positionX,
            positionY = clip.positionY,
            opacity = clip.opacity,
            textStyle = clip.textStyle,
            effectId = clip.effectId,
            transitionIn = clip.transitionIn,
            transitionOut = clip.transitionOut,
            colorFilter = clip.colorFilter,
            mask = clip.mask,
            blendMode = clip.blendMode,
            sticker = clip.sticker,
            animationIn = clip.animationIn,
            animationOut = clip.animationOut,
            colorGrade = clip.colorGrade,
            audioEQ = clip.audioEQ,
            freezeDurationMs = clip.freezeDurationMs
        )
    }

    fun pasteStyleToSelection() {
        val clipboard = styleClipboard ?: return
        val selectedIds = trackManager.selectedClipIds.value.ifEmpty {
            trackManager.selectedClipId.value?.let { setOf(it) } ?: emptySet()
        }
        if (selectedIds.isEmpty()) return
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Paste style to selection") {
                trackManager.tracks.value.forEachIndexed { trackIndex, track ->
                    track.clips
                        .filter { it.id in selectedIds }
                        .forEach { clip ->
                            val canPasteTextRole = clipboard.sourceTrackType == TrackType.TEXT && track.type == TrackType.TEXT
                            val canPasteSticker = clipboard.sourceTrackType == TrackType.STICKER && track.type == TrackType.STICKER
                            val canPasteAudioStyle = clipboard.sourceTrackType == TrackType.AUDIO || track.type == TrackType.AUDIO
                            val canPasteVisualStyle = track.type in setOf(TrackType.VIDEO, TrackType.OVERLAY, TrackType.STICKER, TrackType.TEXT)
                            trackManager.updateClip(
                                trackIndex,
                                clip.copy(
                                    contentRole = if (canPasteTextRole || canPasteSticker) {
                                        clipboard.contentRole ?: clip.contentRole
                                    } else {
                                        clip.contentRole
                                    },
                                    volume = if (canPasteAudioStyle || canPasteVisualStyle) clipboard.volume else clip.volume,
                                    audioFadeInMs = if (canPasteAudioStyle) clipboard.audioFadeInMs else clip.audioFadeInMs,
                                    audioFadeOutMs = if (canPasteAudioStyle) clipboard.audioFadeOutMs else clip.audioFadeOutMs,
                                    speed = clipboard.speed,
                                    rotation = if (canPasteVisualStyle) clipboard.rotation else clip.rotation,
                                    scaleX = if (canPasteVisualStyle) clipboard.scaleX else clip.scaleX,
                                    scaleY = if (canPasteVisualStyle) clipboard.scaleY else clip.scaleY,
                                    positionX = if (canPasteVisualStyle) clipboard.positionX else clip.positionX,
                                    positionY = if (canPasteVisualStyle) clipboard.positionY else clip.positionY,
                                    opacity = if (canPasteVisualStyle) clipboard.opacity else clip.opacity,
                                    textStyle = if (canPasteTextRole) clipboard.textStyle ?: clip.textStyle else clip.textStyle,
                                    effectId = clipboard.effectId,
                                    transitionIn = clipboard.transitionIn,
                                    transitionOut = clipboard.transitionOut,
                                    colorFilter = clipboard.colorFilter,
                                    mask = if (canPasteVisualStyle) clipboard.mask else clip.mask,
                                    blendMode = if (canPasteVisualStyle) clipboard.blendMode else clip.blendMode,
                                    sticker = if (canPasteSticker) clipboard.sticker ?: clip.sticker else clip.sticker,
                                    animationIn = clipboard.animationIn,
                                    animationOut = clipboard.animationOut,
                                    colorGrade = if (canPasteVisualStyle) clipboard.colorGrade else clip.colorGrade,
                                    audioEQ = if (canPasteAudioStyle) clipboard.audioEQ else clip.audioEQ,
                                    freezeDurationMs = if (canPasteVisualStyle) clipboard.freezeDurationMs else clip.freezeDurationMs
                                )
                            )
                        }
                }
            }
        )
    }

    fun undo() {
        undoRedoManager.undo()
    }

    fun redo() {
        undoRedoManager.redo()
    }

    fun addTrack(type: String) {
        val trackType = try { TrackType.valueOf(type.uppercase()) } catch (_: Exception) { TrackType.VIDEO }
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Add track") {
                trackManager.addTrack(trackType, type.replaceFirstChar { it.uppercase() })
            }
        )
    }

    fun toggleTrackVisibility(index: Int) {
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Toggle track visibility") {
                trackManager.toggleTrackVisibility(index)
            }
        )
    }

    fun toggleTrackMute(index: Int) {
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Toggle track mute") {
                trackManager.toggleTrackMute(index)
            }
        )
    }

    fun toggleTrackLock(index: Int) {
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Toggle track lock") {
                trackManager.toggleTrackLock(index)
            }
        )
    }

    fun soloTrack(index: Int) {
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Solo track") {
                trackManager.soloTrack(index)
            }
        )
    }

    fun showAllTracks() {
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Show all tracks") {
                trackManager.showAllTracks()
            }
        )
    }

    fun applyChromaKeyToSelected(
        colorHex: String,
        similarity: Float = 0.4f,
        smoothness: Float = 0.1f
    ) {
        val clipId = trackManager.selectedClipId.value ?: return
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Apply chroma key") {
                trackManager.applyEffect(clipId, "chromakey")
                trackManager.applyColorFilter(
                    clipId,
                    "chromakey:${colorHex.removePrefix("0x").removePrefix("#")}:${similarity.coerceIn(0f, 1f)}:${smoothness.coerceIn(0f, 1f)}"
                )
            }
        )
    }

    fun clearChromaKeyFromSelected() {
        val clipId = trackManager.selectedClipId.value ?: return
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Clear chroma key") {
                trackManager.removeEffect(clipId)
                if (trackManager.getClip(clipId)?.colorFilter?.startsWith("chromakey:", ignoreCase = true) == true) {
                    trackManager.applyColorFilter(clipId, "")
                }
            }
        )
    }

    fun applyMaskAndBlendToSelected(mask: com.changecut.core.editor.MaskDef?, blend: com.changecut.core.editor.BlendMode) {
        val clipId = trackManager.selectedClipId.value ?: return
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Apply mask and blend") {
                trackManager.applyMask(clipId, mask)
                trackManager.applyBlendMode(clipId, blend)
            }
        )
    }

    fun applyStickerToSelected(sticker: StickerDef?) {
        val clipId = trackManager.selectedClipId.value ?: return
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Apply sticker") {
                trackManager.applySticker(clipId, sticker)
            }
        )
    }

    fun applyAnimationToSelected(
        animationIn: com.changecut.core.editor.AnimationDef?,
        animationOut: com.changecut.core.editor.AnimationDef?
    ) {
        val clipId = trackManager.selectedClipId.value ?: return
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Apply animation") {
                trackManager.applyAnimation(clipId, animationIn, animationOut)
            }
        )
    }

    fun applyColorGradeToSelected(grade: com.changecut.core.editor.ColorGradeDef?) {
        val clipId = trackManager.selectedClipId.value ?: return
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Apply color grade") {
                trackManager.applyColorGrade(clipId, grade)
            }
        )
    }

    fun applyAudioEqToSelected(eq: com.changecut.core.editor.AudioEQDef?) {
        val clipId = trackManager.selectedClipId.value ?: return
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Apply audio EQ") {
                trackManager.applyAudioEQ(clipId, eq)
            }
        )
    }

    fun deleteGroup(groupId: String) {
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Delete group") {
                trackManager.deleteGroup(groupId)
            }
        )
    }

    fun toggleGroupExpand(groupId: String) {
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Toggle group") {
                trackManager.toggleGroupExpand(groupId)
            }
        )
    }

    fun navigateTo(screen: String) {
        _currentSubScreen.value = screen
    }

    fun navigateToTextOverlay() { navigateTo("text") }
    fun navigateToAudio() { navigateTo("audio") }
    fun navigateToEffects() { navigateTo("effects") }
    fun navigateToTransition() { navigateTo("transition") }
    fun navigateToTransform() { navigateTo("transform") }
    fun navigateToKeyframe() { navigateTo("keyframe") }
    fun navigateToChromaKey() { navigateTo("chromakey") }
    fun navigateToSpeedRamp() { navigateTo("speedramp") }
    fun navigateToAiAutoEdit() { navigateTo("aiautoedit") }
    fun navigateToAiCaption() { navigateTo("aicaption") }
    fun navigateToAiVoiceOver() { navigateTo("aivoiceover") }
    fun navigateToSubtitle() { navigateTo("subtitle") }
    fun navigateToMask() { navigateTo("mask") }
    fun navigateToSticker() { navigateTo("sticker") }
    fun navigateToAnimation() { navigateTo("animation") }
    fun navigateToColorGrading() { navigateTo("colorgrading") }
    fun navigateToAudioEQ() { navigateTo("audioeq") }
    fun navigateToAdjustment() { navigateTo("adjustment") }
    fun navigateToTemplates() { navigateTo("templates") }
    fun navigateToSettings() { navigateTo("settings") }

    fun navigateToEditor() {
        _currentSubScreen.value = "editor"
    }

    fun createGroupFromSelection() {
        val selectedIds = trackManager.selectedClipIds.value
        val clipIds = when {
            selectedIds.isNotEmpty() -> selectedIds.toList()
            trackManager.selectedClipId.value != null -> listOf(trackManager.selectedClipId.value!!)
            else -> emptyList()
        }
        if (clipIds.isEmpty()) {
            _error.value = "Select at least one clip to create a group"
            return
        }
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Create group") {
                trackManager.createGroup("Group ${trackManager.groups.value.size + 1}", clipIds)
            }
        )
    }

    fun selectGroup(groupId: String) {
        val group = trackManager.groups.value.firstOrNull { it.id == groupId } ?: return
        val existingIds = group.clipIds.filter { clipId -> trackManager.getClip(clipId) != null }
        if (existingIds.isEmpty()) return
        trackManager.setSelection(existingIds.first(), existingIds.toSet())
        val trackIndex = trackManager.tracks.value.indexOfFirst { track -> track.clips.any { it.id == existingIds.first() } }
        if (trackIndex >= 0) {
            trackManager.selectTrack(trackIndex)
        }
    }

    fun addAdjustmentTrack() {
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Add adjustment track") {
                trackManager.addAdjustmentTrack()
            }
        )
    }

    fun clearClipSelection() {
        trackManager.clearSelection()
    }

    fun toggleSnap() {
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Toggle snap") {
                trackManager.toggleSnap()
            }
        )
    }

    fun selectAllOnCurrentTrack() {
        trackManager.selectAllClipsOnTrack(trackManager.selectedTrackIndex.value)
    }

    fun selectAllOnTrack(index: Int) {
        trackManager.selectAllClipsOnTrack(index)
    }

    fun updateSelectedClipTransform(
        positionX: Float? = null,
        positionY: Float? = null,
        scaleX: Float? = null,
        scaleY: Float? = null,
        rotation: Float? = null,
        opacity: Float? = null
    ) {
        val clipId = trackManager.selectedClipId.value ?: return
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Update clip transform") {
                trackManager.updateClipTransform(
                    clipId = clipId,
                    positionX = positionX,
                    positionY = positionY,
                    scaleX = scaleX,
                    scaleY = scaleY,
                    rotation = rotation,
                    opacity = opacity
                )
            }
        )
    }

    fun applySelectedClipTransition(
        transitionIn: TransitionDef? = null,
        transitionOut: TransitionDef? = null,
        replaceTransitionOut: Boolean = false
    ) {
        val clipId = trackManager.selectedClipId.value ?: return
        val clip = trackManager.getClip(clipId) ?: return
        val ownerTrack = trackManager.tracks.value.firstOrNull { track -> track.clips.any { it.id == clipId } }
        val orderedTrackClips = ownerTrack?.clips?.sortedBy { it.startOffsetUs }.orEmpty()
        val clipIndex = orderedTrackClips.indexOfFirst { it.id == clipId }
        val nextClip = orderedTrackClips.getOrNull(clipIndex + 1)
        val resolvedTransitionOut = if (replaceTransitionOut) transitionOut else (transitionOut ?: clip.transitionOut)
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Apply transition") {
                trackManager.applyTransition(
                    clipId = clipId,
                    transitionIn = transitionIn ?: clip.transitionIn,
                    transitionOut = resolvedTransitionOut
                )
                if (nextClip != null && replaceTransitionOut) {
                    trackManager.applyTransition(
                        clipId = nextClip.id,
                        transitionIn = transitionOut?.copy(id = "${transitionOut.id}_in"),
                        transitionOut = nextClip.transitionOut
                    )
                }
            }
        )
    }

    fun setSelectedClipFreeze(durationMs: Int) {
        val clipId = trackManager.selectedClipId.value ?: return
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Set freeze duration") {
                trackManager.setFreezeDuration(clipId, durationMs)
            }
        )
    }

    fun addStickerClip(sticker: StickerDef) {
        val newClip = EditorClip(
            id = UUID.randomUUID().toString(),
            sourceUri = sticker.assetPath ?: "",
            label = sticker.name,
            contentRole = "sticker",
            startOffsetUs = 0L,
            endOffsetUs = 3_000_000L,
            positionX = 0.5f,
            positionY = 0.5f,
            scaleX = 0.2f,
            scaleY = 0.2f,
            sticker = sticker
        )
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Add sticker clip") {
                var trackIndex = trackManager.tracks.value.indexOfFirst { it.type == TrackType.STICKER }
                if (trackIndex < 0) {
                    trackManager.addTrack(TrackType.STICKER, "Sticker")
                    trackIndex = trackManager.tracks.value.indexOfFirst { it.type == TrackType.STICKER }
                }
                if (trackIndex < 0) return@SnapshotCommand
                val currentTimeUs = trackManager.currentTimeUs.value
                trackManager.addClip(
                    trackIndex,
                    newClip.copy(
                        startOffsetUs = currentTimeUs,
                        endOffsetUs = currentTimeUs + 3_000_000L
                    )
                )
                trackManager.selectClip(newClip.id)
                trackManager.selectTrack(trackIndex)
            }
        )
    }

    fun getClipKeyframes(clipId: String): List<KeyframeItem> {
        return trackManager.getClip(clipId)
            ?.keyframes
            ?.map {
                KeyframeItem(
                    id = it.id,
                    property = it.property,
                    timeUs = it.timeUs,
                    value = it.value,
                    easing = it.easing
                )
            }
            ?: emptyList()
    }

    fun saveClipKeyframes(clipId: String, items: List<KeyframeItem>) {
        val keyframes = items.map {
            com.changecut.core.editor.Keyframe(
                id = it.id,
                property = it.property,
                timeUs = it.timeUs,
                value = it.value,
                easing = it.easing
            )
        }
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Save keyframes") {
                trackManager.setClipKeyframes(clipId, keyframes)
            }
        )
    }

    fun getClipSpeedRamp(clipId: String): List<SpeedControlPoint> {
        val clip = trackManager.getClip(clipId) ?: return emptyList()
        val durationUs = (clip.trimEndUs - clip.trimStartUs).takeIf { it > 0 } ?: clip.durationUs
        if (durationUs <= 0) return emptyList()
        return clip.keyframes
            .filter { it.property == SPEED_RAMP_PROPERTY }
            .sortedBy { it.timeUs }
            .map {
                SpeedControlPoint(
                    id = it.id,
                    timeFraction = (it.timeUs.toFloat() / durationUs.toFloat()).coerceIn(0f, 1f),
                    speed = it.value
                )
            }
    }

    fun saveClipSpeedRamp(clipId: String, points: List<SpeedControlPoint>) {
        val clip = trackManager.getClip(clipId) ?: return
        val durationUs = (clip.trimEndUs - clip.trimStartUs).takeIf { it > 0 } ?: clip.durationUs
        if (durationUs <= 0) return

        val nonSpeedKeyframes = clip.keyframes.filter { it.property != SPEED_RAMP_PROPERTY }
        val speedKeyframes = points
            .sortedBy { it.timeFraction }
            .map {
                com.changecut.core.editor.Keyframe(
                    id = it.id,
                    property = SPEED_RAMP_PROPERTY,
                    timeUs = (durationUs * it.timeFraction).toLong(),
                    value = it.speed,
                    easing = com.changecut.core.editor.EasingType.LINEAR
                )
            }
        val weightedSpeed = computeWeightedSpeed(points)
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Save speed ramp") {
                trackManager.setClipKeyframes(clipId, nonSpeedKeyframes + speedKeyframes)
                val updated = clip.copy(speed = weightedSpeed)
                val tracks = trackManager.tracks.value
                for ((index, track) in tracks.withIndex()) {
                    if (track.clips.any { it.id == clipId }) {
                        trackManager.updateClip(index, updated.copy(keyframes = nonSpeedKeyframes + speedKeyframes))
                        return@SnapshotCommand
                    }
                }
            }
        )
    }

    private fun computeWeightedSpeed(points: List<SpeedControlPoint>): Float {
        if (points.isEmpty()) return 1f
        val sorted = points.sortedBy { it.timeFraction }
        if (sorted.size == 1) return sorted.first().speed
        var weightedSum = 0f
        var totalWeight = 0f
        for (i in 0 until sorted.lastIndex) {
            val start = sorted[i]
            val end = sorted[i + 1]
            val span = (end.timeFraction - start.timeFraction).coerceAtLeast(0f)
            weightedSum += start.speed * span
            totalWeight += span
        }
        return if (totalWeight > 0f) (weightedSum / totalWeight).coerceIn(0.1f, 10f) else sorted.first().speed
    }

    fun setZoom(level: Float) {
        trackManager.setZoom(level)
    }

    fun applyTemplate(template: TemplateProject) {
        val tracks = when {
            template.tracks.isNotEmpty() -> template.tracks
            else -> buildFallbackTemplateTracks(template)
        }
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Apply template") {
                trackManager.replaceAllTracks(tracks)
            }
        )
        _isPlaying.value = false
        frameScheduler.pause()
        frameScheduler.seekTo(0L)
        navigateToEditor()
    }

    fun saveCurrentTimelineAsTemplate(
        name: String,
        category: String = "Custom",
        description: String = ""
    ) {
        val template = TemplateProject(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            category = category,
            tracks = trackManager.tracks.value,
            durationMs = (trackManager.totalDurationUs.value / 1000L)
        )
        templateEngine.saveTemplate(template)
            .onFailure { _error.value = it.message ?: "Failed to save template" }
    }

    fun exportVideo(width: Int = 1080, height: Int = 1920, fps: Int = 30) {
        val project = _project.value ?: return
        val clips = trackManager.tracks.value.flatMap { it.clips }
        if (clips.isEmpty()) {
            _error.value = "No clips to export"
            return
        }

        _isExporting.value = true
        _exportProgress.value = 0f
        viewModelScope.launch {
            try {
                val exportDir = mediaProjectManager.getExportDir(project.id)
                val outputFile = File(exportDir, "${project.name}_export.mp4")
                val outputPath = outputFile.absolutePath

                val timelineExport = videoEngine.exportTimeline(
                    trackManager = trackManager,
                    outputPath = outputPath,
                    width = width,
                    height = height,
                    fps = fps,
                    onProgress = { progress ->
                        _exportProgress.value = progress
                    }
                )

                val exportResult = if (timelineExport.isSuccess) {
                    timelineExport
                } else {
                    hardwareExporter.export(
                        trackManager = trackManager,
                        outputPath = outputPath,
                        width = width,
                        height = height,
                        frameRate = fps,
                        onProgress = { progress ->
                            _exportProgress.value = progress
                        }
                    )
                }

                exportResult.onSuccess {
                    _isExporting.value = false
                    _exportProgress.value = 1f
                    _exportOutputPath.value = outputPath
                }.onFailure { e ->
                    _isExporting.value = false
                    _error.value = e.message ?: "Export failed"
                }
            } catch (e: Exception) {
                _isExporting.value = false
                _error.value = e.message ?: "Export failed"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearExportOutput() {
        _exportOutputPath.value = null
    }

    private fun buildFallbackTemplateTracks(template: TemplateProject): List<Track> {
        val durationUs = (template.durationMs.coerceAtLeast(3_000L)) * 1000L
        val textClip = EditorClip(
            id = UUID.randomUUID().toString(),
            sourceUri = "",
            label = "${template.name} Title",
            contentRole = "title",
            startOffsetUs = 0L,
            endOffsetUs = durationUs,
            textStyle = defaultAutoTextStyle(position = "title"),
            positionX = 0.5f,
            positionY = 0.75f,
            textContent = template.name
        )
        return listOf(
            Track(id = "video_0", type = TrackType.VIDEO, label = "Video"),
            Track(id = "audio_0", type = TrackType.AUDIO, label = "Audio"),
            Track(id = "text_0", type = TrackType.TEXT, label = "Text", clips = listOf(textClip)),
            Track(id = "overlay_0", type = TrackType.OVERLAY, label = "Overlay")
        )
    }

    private fun defaultAutoTextStyle(position: String): TextClipStyle {
        return when (position) {
            "subtitle" -> TextClipStyle(
                fontSize = 32f,
                color = 0xFFFFFFFFL,
                backgroundColor = 0xAA000000L,
                alignment = 2,
                shadow = true,
                outline = true
            )
            "title" -> TextClipStyle(
                fontSize = 42f,
                color = 0xFFFFFFFFL,
                alignment = 1,
                shadow = true,
                outline = true
            )
            else -> TextClipStyle(
                fontSize = 36f,
                color = 0xFFFFFFFFL,
                backgroundColor = 0x66000000L,
                alignment = 2,
                shadow = true,
                outline = true
            )
        }
    }

    private fun removeExistingSubtitleClips() {
        val tracks = trackManager.tracks.value
        tracks.forEachIndexed { trackIndex, track ->
            if (track.type != TrackType.TEXT) return@forEachIndexed
            track.clips
                .filter { isSubtitleClip(it) }
                .forEach { clip -> trackManager.removeClip(trackIndex, clip.id) }
        }
    }

    private fun isSubtitleClip(clip: EditorClip): Boolean {
        return clip.contentRole == "subtitle" ||
            clip.label.startsWith("Subtitle:") ||
            (clip.positionY >= 0.84f && !clip.textContent.isNullOrBlank())
    }

    private fun duplicateClipForTimeline(clip: EditorClip): EditorClip {
        return clip.copy(
            keyframes = clip.keyframes.map { keyframe ->
                keyframe.copy(id = UUID.randomUUID().toString())
            }
        )
    }

    private fun buildDuplicatedLabel(label: String): String {
        val trimmed = label.trim()
        return if (trimmed.endsWith(" Copy", ignoreCase = true)) trimmed else "$trimmed Copy"
    }

    private fun importMediaInternal(uri: Uri, mimeType: String, wrapInHistory: Boolean = true) {
        val isSupported = when {
            mimeType.startsWith("video/") -> mediaUtils.isVideoSupported(mimeType)
            mimeType.startsWith("audio/") -> mediaUtils.isAudioSupported(mimeType)
            mimeType.startsWith("image/") -> mediaUtils.isImageSupported(mimeType)
            else -> false
        }
        if (!isSupported) {
            _error.value = "Unsupported media type: $mimeType"
            return
        }
        val sourceLabel = mediaUtils.queryMediaFromUri(uri)?.name
            ?.substringBeforeLast('.')
            ?.takeIf { it.isNotBlank() }
            ?: File(uri.lastPathSegment ?: "").nameWithoutExtension.takeIf { it.isNotBlank() }
            ?: if (mimeType.startsWith("image/")) "Overlay" else "Clip"
        val fileName = mediaUtils.buildImportedFileName(
            uri = uri,
            prefix = "media",
            mimeType = mimeType,
            fallbackExtension = "mp4"
        )
        val copiedFile = mediaUtils.copyToCache(uri, fileName)
        if (copiedFile == null) {
            _error.value = "Failed to import media"
            return
        }

        val mediaInfo = videoEngine.getMediaInfo(copiedFile.absolutePath)
        val projectId = _project.value?.id
        if (projectId == null) {
            copiedFile.delete()
            return
        }
        val mediaDir = mediaProjectManager.getMediaDir(projectId)
        val finalFile = File(mediaDir, fileName)
        val storedFile = moveImportedFile(copiedFile, finalFile)
        if (storedFile == null) {
            copiedFile.delete()
            _error.value = "Failed to store imported media"
            return
        }

        val clipDurationUs = (mediaInfo?.durationMs ?: 0L) * 1000L
        val baseClip = EditorClip(
            id = UUID.randomUUID().toString(),
            sourceUri = storedFile.absolutePath,
            label = sourceLabel,
            startOffsetUs = 0L,
            endOffsetUs = clipDurationUs.coerceAtLeast(if (mimeType.startsWith("image/")) 3_000_000L else 1_000_000L)
        )
        val applyImport = importBlock@{
            val trackIndex = ensureImportTrackIndex(mimeType)
            val targetTrack = trackManager.tracks.value.getOrNull(trackIndex) ?: return@importBlock
            val timelineStartUs = targetTrack.clips.maxOfOrNull { it.endOffsetUs } ?: 0L
            val fallbackDurationUs = if (mimeType.startsWith("image/")) 3_000_000L else 1_000_000L
            val targetTrackType = targetTrack.type
            val clip = baseClip.copy(
                contentRole = when {
                    mimeType.startsWith("image/") && targetTrackType == TrackType.STICKER -> "sticker"
                    mimeType.startsWith("image/") -> "overlay"
                    targetTrackType == TrackType.OVERLAY -> "overlay_video"
                    else -> null
                },
                startOffsetUs = timelineStartUs,
                endOffsetUs = timelineStartUs + clipDurationUs.coerceAtLeast(fallbackDurationUs)
            )
            trackManager.addClip(trackIndex, clip)
            trackManager.selectTrack(trackIndex)
            trackManager.selectClip(clip.id)
        }
        if (wrapInHistory) {
            undoRedoManager.execute(
                SnapshotCommand(trackManager, "Import media") {
                    applyImport()
                }
            )
        } else {
            applyImport()
        }
    }

    private fun moveImportedFile(source: File, target: File): File? {
        return try {
            target.parentFile?.mkdirs()
            if (target.exists()) {
                target.delete()
            }
            val moved = if (source.absolutePath == target.absolutePath) {
                true
            } else {
                source.renameTo(target)
            }
            if (moved) {
                target
            } else {
                source.copyTo(target, overwrite = true)
                source.delete()
                target
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun ensureImportTrackIndex(mimeType: String): Int {
        val resolvedTrackType = resolvePreferredImportTrackType(mimeType)
        val existingIndex = resolveImportTrackIndex(mimeType)
        if (existingIndex >= 0) {
            return existingIndex
        }
        val newLabel = when (resolvedTrackType) {
            TrackType.VIDEO -> "Video"
            TrackType.OVERLAY -> "Overlay"
            TrackType.STICKER -> "Sticker"
            TrackType.AUDIO -> "Audio"
            TrackType.TEXT -> "Text"
            TrackType.EFFECT -> "Effect"
            TrackType.ADJUSTMENT -> "Adjustment"
        }
        trackManager.addTrack(resolvedTrackType, newLabel)
        return trackManager.tracks.value.indexOfLast { it.type == resolvedTrackType }
            .takeIf { it >= 0 }
            ?: 0
    }

    private fun resolveImportTrackIndex(mimeType: String): Int {
        val selectedIndex = trackManager.selectedTrackIndex.value
        val tracks = trackManager.tracks.value
        val selectedTrack = tracks.getOrNull(selectedIndex)
        return when {
            mimeType.startsWith("video/") && selectedTrack?.type in setOf(TrackType.VIDEO, TrackType.OVERLAY) -> selectedIndex
            mimeType.startsWith("video/") -> tracks.indexOfFirst { it.type == TrackType.VIDEO }
                .takeIf { it >= 0 }
                ?: tracks.indexOfFirst { it.type == TrackType.OVERLAY }.takeIf { it >= 0 }
            mimeType.startsWith("image/") && selectedTrack?.type in setOf(TrackType.OVERLAY, TrackType.STICKER) -> selectedIndex
            mimeType.startsWith("image/") -> tracks.indexOfFirst { it.type == TrackType.OVERLAY }
                .takeIf { it >= 0 }
                ?: tracks.indexOfFirst { it.type == TrackType.STICKER }.takeIf { it >= 0 }
            else -> selectedIndex.takeIf { it in tracks.indices } ?: -1
        } ?: -1
    }

    private fun resolvePreferredImportTrackType(mimeType: String): TrackType {
        val selectedTrack = trackManager.tracks.value.getOrNull(trackManager.selectedTrackIndex.value)
        return when {
            mimeType.startsWith("image/") && selectedTrack?.type == TrackType.STICKER -> TrackType.STICKER
            mimeType.startsWith("image/") -> TrackType.OVERLAY
            mimeType.startsWith("video/") && selectedTrack?.type == TrackType.OVERLAY -> TrackType.OVERLAY
            mimeType.startsWith("video/") -> TrackType.VIDEO
            mimeType.startsWith("audio/") -> TrackType.AUDIO
            else -> selectedTrack?.type ?: TrackType.VIDEO
        }
    }
}

private const val SPEED_RAMP_PROPERTY = "SPEED_RAMP"
