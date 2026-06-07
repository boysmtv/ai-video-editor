package com.changecut.core.editor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs
import javax.inject.Inject
import javax.inject.Singleton

data class Track(
    val id: String,
    val type: TrackType,
    val label: String,
    val clips: List<EditorClip> = emptyList(),
    val isVisible: Boolean = true,
    val isMuted: Boolean = false,
    val isLocked: Boolean = false
)

enum class TrackType {
    VIDEO, AUDIO, TEXT, OVERLAY, EFFECT, STICKER, ADJUSTMENT
}

data class EditorClip(
    val id: String,
    val sourceUri: String,
    val label: String = "Clip",
    val contentRole: String? = null,
    val startOffsetUs: Long = 0L,
    val endOffsetUs: Long = 0L,
    val trimStartUs: Long = 0L,
    val trimEndUs: Long = 0L,
    val volume: Float = 1.0f,
    val audioFadeInMs: Int = 0,
    val audioFadeOutMs: Int = 0,
    val speed: Float = 1.0f,
    val rotation: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val opacity: Float = 1f,
    val textContent: String? = null,
    val textStyle: TextClipStyle? = null,
    val effectId: String? = null,
    val transitionIn: TransitionDef? = null,
    val transitionOut: TransitionDef? = null,
    val keyframes: List<Keyframe> = emptyList(),
    val colorFilter: String? = null,
    val mask: MaskDef? = null,
    val blendMode: BlendMode = BlendMode.NORMAL,
    val sticker: StickerDef? = null,
    val animationIn: AnimationDef? = null,
    val animationOut: AnimationDef? = null,
    val colorGrade: ColorGradeDef? = null,
    val audioEQ: AudioEQDef? = null,
    val freezeDurationMs: Int = 0
) {
    val durationUs: Long get() = (endOffsetUs - startOffsetUs).coerceAtLeast(0L)
}

data class TextClipStyle(
    val fontName: String = "Default",
    val fontSize: Float = 24f,
    val color: Long = 0xFFFFFFFF,
    val backgroundColor: Long? = null,
    val alignment: Int = 1,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val shadow: Boolean = false,
    val outline: Boolean = false,
    val outlineColor: Long = 0xFF000000,
    val animationIn: String? = null,
    val animationOut: String? = null
)

data class TransitionDef(
    val id: String,
    val type: String = "fade",
    val durationMs: Int = 300
)

data class Keyframe(
    val id: String,
    val property: String,
    val timeUs: Long,
    val value: Float,
    val easing: EasingType = EasingType.LINEAR
)

enum class EasingType { LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT }

data class EffectDef(
    val id: String,
    val name: String,
    val category: EffectCategory,
    val ffmpegFilter: String = ""
)

enum class EffectCategory {
    COLOR, BLUR, GLITCH, SHATTER, SHAKE, ZOOM, BLEND
}

data class FilterDef(
    val id: String,
    val name: String,
    val ffmpegFilter: String
)

data class TransitionDefFull(
    val id: String,
    val name: String,
    val ffmpegFilter: String
)

data class TimelineSnapshot(
    val tracks: List<Track>,
    val groups: List<ClipGroup>,
    val currentTimeUs: Long,
    val selectedClipId: String?,
    val selectedClipIds: Set<String>,
    val selectedTrackIndex: Int,
    val zoomLevel: Float,
    val snapEnabled: Boolean
)

@Singleton
class TrackManager @Inject constructor() {
    private val _tracks = MutableStateFlow(
        listOf(
            Track(id = "video_0", type = TrackType.VIDEO, label = "Video"),
            Track(id = "audio_0", type = TrackType.AUDIO, label = "Audio"),
            Track(id = "text_0", type = TrackType.TEXT, label = "Text"),
            Track(id = "overlay_0", type = TrackType.OVERLAY, label = "Overlay")
        )
    )
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _currentTimeUs = MutableStateFlow(0L)
    val currentTimeUs: StateFlow<Long> = _currentTimeUs.asStateFlow()

    private val _totalDurationUs = MutableStateFlow(0L)
    val totalDurationUs: StateFlow<Long> = _totalDurationUs.asStateFlow()

    private val _selectedClipId = MutableStateFlow<String?>(null)
    private val _groups = MutableStateFlow<List<ClipGroup>>(emptyList())
    val groups: StateFlow<List<ClipGroup>> = _groups.asStateFlow()
    private val _selectedClipIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedClipIds: StateFlow<Set<String>> = _selectedClipIds.asStateFlow()
    private val _snapEnabled = MutableStateFlow(true)
    val snapEnabled: StateFlow<Boolean> = _snapEnabled.asStateFlow()
    val selectedClipId: StateFlow<String?> = _selectedClipId.asStateFlow()

    private val _selectedTrackIndex = MutableStateFlow(0)
    val selectedTrackIndex: StateFlow<Int> = _selectedTrackIndex.asStateFlow()

    private val _zoomLevel = MutableStateFlow(1f)
    val zoomLevel: StateFlow<Float> = _zoomLevel.asStateFlow()

    fun seekTo(timeUs: Long) {
        _currentTimeUs.value = timeUs.coerceIn(0, _totalDurationUs.value)
    }

    fun setZoom(level: Float) {
        _zoomLevel.value = level.coerceIn(0.1f, 10f)
    }

    fun setSnapEnabled(enabled: Boolean) {
        _snapEnabled.value = enabled
    }

    fun addTrack(type: TrackType, label: String) {
        val id = "${type.name.lowercase()}_${_tracks.value.size}"
        _tracks.update { it + Track(id = id, type = type, label = label) }
    }

    fun toggleTrackVisibility(index: Int) {
        _tracks.update { tracks ->
            tracks.toMutableList().also { list ->
                list[index] = list[index].copy(isVisible = !list[index].isVisible)
            }
        }
    }

    fun toggleTrackMute(index: Int) {
        _tracks.update { tracks ->
            tracks.toMutableList().also { list ->
                list[index] = list[index].copy(isMuted = !list[index].isMuted)
            }
        }
    }

    fun toggleTrackLock(index: Int) {
        _tracks.update { tracks ->
            tracks.toMutableList().also { list ->
                list[index] = list[index].copy(isLocked = !list[index].isLocked)
            }
        }
    }

    fun soloTrack(index: Int) {
        _tracks.update { tracks ->
            tracks.mapIndexed { trackIndex, track ->
                track.copy(isVisible = trackIndex == index)
            }
        }
    }

    fun showAllTracks() {
        _tracks.update { tracks ->
            tracks.map { track -> track.copy(isVisible = true) }
        }
    }

    fun addClip(trackIndex: Int, clip: EditorClip) {
        _tracks.update { tracks ->
            tracks.toMutableList().also { list ->
                val track = list[trackIndex]
                list[trackIndex] = track.copy(
                    clips = (track.clips + clip).sortedWith(compareBy<EditorClip> { it.startOffsetUs }.thenBy { it.endOffsetUs })
                )
            }
        }
        recalculateDuration()
    }

    fun removeClip(trackIndex: Int, clipId: String) {
        _tracks.update { tracks ->
            tracks.toMutableList().also { list ->
                val track = list[trackIndex]
                list[trackIndex] = track.copy(clips = track.clips.filter { it.id != clipId })
            }
        }
        if (_selectedClipId.value == clipId) {
            _selectedClipId.value = null
        }
        if (_selectedClipIds.value.contains(clipId)) {
            _selectedClipIds.value = _selectedClipIds.value - clipId
        }
        _groups.value = _groups.value.mapNotNull { group ->
            val remainingIds = group.clipIds.filter { it != clipId }
            when {
                remainingIds.isEmpty() -> null
                else -> group.copy(clipIds = remainingIds)
            }
        }
        recalculateDuration()
    }

    fun updateClip(trackIndex: Int, clip: EditorClip) {
        _tracks.update { tracks ->
            tracks.toMutableList().also { list ->
                val track = list[trackIndex]
                list[trackIndex] = track.copy(
                    clips = track.clips
                        .map { if (it.id == clip.id) clip else it }
                        .sortedWith(compareBy<EditorClip> { it.startOffsetUs }.thenBy { it.endOffsetUs })
                )
            }
        }
        recalculateDuration()
    }

    fun moveClipInTime(trackIndex: Int, clipId: String, newStartOffsetUs: Long) {
        val track = _tracks.value.getOrNull(trackIndex) ?: return
        val clip = track.clips.find { it.id == clipId } ?: return
        val durationUs = (clip.endOffsetUs - clip.startOffsetUs).coerceAtLeast(0L)
        val rawStartUs = newStartOffsetUs.coerceAtLeast(0L)
        val snappedStartUs = if (_snapEnabled.value) snapTimeUs(trackIndex, clipId, rawStartUs) else rawStartUs
        val updated = clip.copy(
            startOffsetUs = snappedStartUs,
            endOffsetUs = snappedStartUs + durationUs
        )
        updateClip(trackIndex, updated)
    }

    fun moveSelectedClipsInTime(deltaUs: Long) {
        val selectedIds = _selectedClipIds.value.ifEmpty {
            _selectedClipId.value?.let { setOf(it) } ?: emptySet()
        }
        if (selectedIds.isEmpty()) return

        val selectedClips = getAllClips().filter { it.id in selectedIds }
        if (selectedClips.isEmpty()) return
        val earliestSelected = selectedClips.minByOrNull { it.startOffsetUs } ?: return
        val rawEarliestStartUs = (earliestSelected.startOffsetUs + deltaUs).coerceAtLeast(0L)
        val effectiveDeltaUs = if (_snapEnabled.value) {
            val ownerTrackIndex = _tracks.value.indexOfFirst { track -> track.clips.any { it.id == earliestSelected.id } }
            if (ownerTrackIndex >= 0) {
                snapTimeUs(ownerTrackIndex, earliestSelected.id, rawEarliestStartUs) - earliestSelected.startOffsetUs
            } else {
                deltaUs
            }
        } else {
            deltaUs
        }

        _tracks.update { tracks ->
            tracks.map { track ->
                val selectedTrackClips = track.clips.filter { it.id in selectedIds }
                if (selectedTrackClips.isEmpty()) {
                    track
                } else {
                    val adjustedClips = track.clips.map { clip ->
                        if (clip.id !in selectedIds) {
                            clip
                        } else {
                            val snappedStartUs = (clip.startOffsetUs + effectiveDeltaUs).coerceAtLeast(0L)
                            val durationUs = clip.durationUs.coerceAtLeast(0L)
                            clip.copy(
                                startOffsetUs = snappedStartUs,
                                endOffsetUs = snappedStartUs + durationUs
                            )
                        }
                    }
                    track.copy(clips = adjustedClips)
                }
            }
        }
        recalculateDuration()
    }

    fun trimClipBounds(trackIndex: Int, clipId: String, newStartOffsetUs: Long, newEndOffsetUs: Long) {
        val track = _tracks.value.getOrNull(trackIndex) ?: return
        val clip = track.clips.find { it.id == clipId } ?: return
        val boundedStartUs = newStartOffsetUs.coerceAtLeast(0L)
        val boundedEndUs = newEndOffsetUs.coerceAtLeast(boundedStartUs + 33_000L)
        val deltaStartUs = (boundedStartUs - clip.startOffsetUs)
        val deltaEndUs = (clip.endOffsetUs - boundedEndUs)
        val speed = clip.speed.coerceAtLeast(0.01f)
        val trimStartUs = (clip.trimStartUs + (deltaStartUs * speed).toLong()).coerceAtLeast(0L)
        val currentTrimEndUs = effectiveTrimEndUs(clip)
        val trimEndUs = (currentTrimEndUs - (deltaEndUs * speed).toLong()).coerceAtLeast(trimStartUs + 33_000L)
        updateClip(
            trackIndex,
            clip.copy(
                startOffsetUs = boundedStartUs,
                endOffsetUs = boundedEndUs,
                trimStartUs = trimStartUs,
                trimEndUs = trimEndUs
            )
        )
    }

    fun moveClip(trackIndex: Int, clipId: String, newIndex: Int) {
        _tracks.update { tracks ->
            tracks.toMutableList().also { list ->
                val track = list[trackIndex]
                val clips = track.clips.toMutableList()
                val clip = clips.find { it.id == clipId } ?: return@also
                clips.remove(clip)
                clips.add(newIndex.coerceIn(0, clips.size), clip)
                list[trackIndex] = track.copy(clips = clips)
            }
        }
    }

    fun splitClip(trackIndex: Int, clipId: String, splitTimeUs: Long): String? {
        val track = _tracks.value.getOrNull(trackIndex) ?: return null
        val clip = track.clips.find { it.id == clipId } ?: return null
        if (splitTimeUs <= clip.startOffsetUs || splitTimeUs >= clip.endOffsetUs) return null
        val newId = "${clip.id}_split"
        val timelineSplitDeltaUs = (splitTimeUs - clip.startOffsetUs).coerceAtLeast(0L)
        val sourceSplitDeltaUs = (timelineSplitDeltaUs * clip.speed.coerceAtLeast(0.01f)).toLong()
        val baseTrimStartUs = clip.trimStartUs.coerceAtLeast(0L)
        val computedSourceSplitUs = baseTrimStartUs + sourceSplitDeltaUs
        val clipTrimEndUs = effectiveTrimEndUs(clip)
        val boundedSourceSplitUs = computedSourceSplitUs.coerceIn(baseTrimStartUs, clipTrimEndUs)

        val first = clip.copy(
            endOffsetUs = splitTimeUs,
            trimEndUs = boundedSourceSplitUs
        )
        val second = clip.copy(
            id = newId,
            startOffsetUs = splitTimeUs,
            trimStartUs = boundedSourceSplitUs
        )
        _tracks.update { tracks ->
            tracks.toMutableList().also { list ->
                val clips = track.clips.toMutableList()
                val idx = clips.indexOfFirst { it.id == clipId }
                clips[idx] = first
                clips.add(idx + 1, second)
                list[trackIndex] = track.copy(clips = clips)
            }
        }
        recalculateDuration()
        return newId
    }

    fun mergeClips(trackIndex: Int, firstId: String, secondId: String) {
        _tracks.update { tracks ->
            tracks.toMutableList().also { list ->
                val track = list[trackIndex]
                val clips = track.clips.toMutableList()
                val first = clips.find { it.id == firstId } ?: return@also
                val second = clips.find { it.id == secondId } ?: return@also
                val mergedTrimEndUs = when {
                    second.trimEndUs > second.trimStartUs -> second.trimEndUs
                    first.trimEndUs > first.trimStartUs -> first.trimEndUs
                    else -> effectiveTrimEndUs(second)
                }
                val merged = first.copy(
                    endOffsetUs = second.endOffsetUs,
                    trimStartUs = first.trimStartUs.coerceAtLeast(0L),
                    trimEndUs = mergedTrimEndUs.coerceAtLeast(first.trimStartUs.coerceAtLeast(0L))
                )
                clips.removeAll { it.id == firstId || it.id == secondId }
                val insertAt = minOf(
                    clips.indexOfFirst { it.startOffsetUs > first.startOffsetUs }.let {
                        if (it == -1) clips.size else it
                    },
                    clips.size
                )
                clips.add(insertAt, merged)
                list[trackIndex] = track.copy(clips = clips)
            }
        }
        _groups.value = _groups.value.map { group ->
            val replacementIds = group.clipIds.flatMap { clipId ->
                when (clipId) {
                    firstId -> listOf(firstId)
                    secondId -> emptyList()
                    else -> listOf(clipId)
                }
            }.distinct()
            group.copy(clipIds = replacementIds)
        }.filter { it.clipIds.isNotEmpty() }
        recalculateDuration()
    }

    fun mergeSelectedClips(): Boolean {
        val selectedIds = _selectedClipIds.value.ifEmpty {
            _selectedClipId.value?.let { setOf(it) } ?: emptySet()
        }
        if (selectedIds.size < 2) return false

        val ownerTrackIndex = _tracks.value.indexOfFirst { track ->
            track.clips.count { it.id in selectedIds } == selectedIds.size
        }
        if (ownerTrackIndex < 0) return false

        val orderedSelected = _tracks.value[ownerTrackIndex].clips
            .filter { it.id in selectedIds }
            .sortedBy { it.startOffsetUs }
        if (orderedSelected.size < 2) return false
        val hasLargeGap = orderedSelected.zipWithNext().any { (first, second) ->
            second.startOffsetUs > first.endOffsetUs + 66_000L
        }
        if (hasLargeGap) return false

        var currentPrimaryId = orderedSelected.first().id
        orderedSelected.drop(1).forEach { clip ->
            mergeClips(ownerTrackIndex, currentPrimaryId, clip.id)
        }
        _selectedTrackIndex.value = ownerTrackIndex
        _selectedClipId.value = currentPrimaryId
        _selectedClipIds.value = setOf(currentPrimaryId)
        return true
    }

    fun selectClip(clipId: String?) {
        _selectedClipId.value = clipId
        _selectedClipIds.value = if (clipId == null) emptySet() else setOf(clipId)
    }

    fun selectTrack(index: Int) {
        _selectedTrackIndex.value = index.coerceIn(0, _tracks.value.size - 1)
    }

    fun selectAllClipsOnTrack(index: Int) {
        val track = _tracks.value.getOrNull(index) ?: return
        val clipIds = track.clips.map { it.id }
        _selectedTrackIndex.value = index.coerceIn(0, _tracks.value.size - 1)
        _selectedClipId.value = clipIds.firstOrNull()
        _selectedClipIds.value = clipIds.toSet()
    }

    fun addKeyframe(clipId: String, keyframe: Keyframe) {
        _tracks.update { tracks ->
            tracks.map { track ->
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id == clipId) clip.copy(keyframes = clip.keyframes + keyframe)
                    else clip
                })
            }
        }
    }

    fun removeKeyframe(clipId: String, keyframeId: String) {
        _tracks.update { tracks ->
            tracks.map { track ->
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id == clipId) clip.copy(keyframes = clip.keyframes.filter { it.id != keyframeId })
                    else clip
                })
            }
        }
    }

    fun setClipKeyframes(clipId: String, keyframes: List<Keyframe>) {
        _tracks.update { tracks ->
            tracks.map { track ->
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id == clipId) clip.copy(keyframes = keyframes.sortedBy { it.timeUs })
                    else clip
                })
            }
        }
    }

    fun applyEffect(clipId: String, effectId: String) {
        _tracks.update { tracks ->
            tracks.map { track ->
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id == clipId) clip.copy(effectId = effectId) else clip
                })
            }
        }
    }

    fun removeEffect(clipId: String) {
        _tracks.update { tracks ->
            tracks.map { track ->
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id == clipId) clip.copy(effectId = null) else clip
                })
            }
        }
    }

    fun applyColorFilter(clipId: String, filterId: String) {
        _tracks.update { tracks ->
            tracks.map { track ->
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id == clipId) clip.copy(colorFilter = filterId.takeIf { it.isNotBlank() }) else clip
                })
            }
        }
    }

    fun updateClipTransform(
        clipId: String,
        positionX: Float? = null,
        positionY: Float? = null,
        scaleX: Float? = null,
        scaleY: Float? = null,
        rotation: Float? = null,
        opacity: Float? = null
    ) {
        _tracks.update { tracks ->
            tracks.map { track ->
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id == clipId) {
                        clip.copy(
                            positionX = positionX ?: clip.positionX,
                            positionY = positionY ?: clip.positionY,
                            scaleX = scaleX ?: clip.scaleX,
                            scaleY = scaleY ?: clip.scaleY,
                            rotation = rotation ?: clip.rotation,
                            opacity = opacity ?: clip.opacity
                        )
                    } else {
                        clip
                    }
                })
            }
        }
    }

    fun applyTransition(clipId: String, transitionIn: TransitionDef?, transitionOut: TransitionDef?) {
        _tracks.update { tracks ->
            tracks.map { track ->
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id == clipId) {
                        clip.copy(transitionIn = transitionIn, transitionOut = transitionOut)
                    } else {
                        clip
                    }
                })
            }
        }
    }

    fun setFreezeDuration(clipId: String, durationMs: Int) {
        _tracks.update { tracks ->
            tracks.map { track ->
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id == clipId) {
                        clip.copy(freezeDurationMs = durationMs.coerceAtLeast(0))
                    } else {
                        clip
                    }
                })
            }
        }
    }

    fun applyMask(clipId: String, mask: MaskDef?) {
        _tracks.update { tracks ->
            tracks.map { track ->
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id == clipId) clip.copy(mask = mask) else clip
                })
            }
        }
    }

    fun applyBlendMode(clipId: String, blendMode: BlendMode) {
        _tracks.update { tracks ->
            tracks.map { track ->
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id == clipId) clip.copy(blendMode = blendMode) else clip
                })
            }
        }
    }

    fun applySticker(clipId: String, sticker: StickerDef?) {
        _tracks.update { tracks ->
            tracks.map { track ->
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id == clipId) clip.copy(sticker = sticker) else clip
                })
            }
        }
    }

    fun applyAnimation(clipId: String, animationIn: AnimationDef?, animationOut: AnimationDef?) {
        _tracks.update { tracks ->
            tracks.map { track ->
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id == clipId) clip.copy(animationIn = animationIn, animationOut = animationOut) else clip
                })
            }
        }
    }

    fun applyColorGrade(clipId: String, grade: ColorGradeDef?) {
        _tracks.update { tracks ->
            tracks.map { track ->
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id == clipId) clip.copy(colorGrade = grade) else clip
                })
            }
        }
    }

    fun applyAudioEQ(clipId: String, eq: AudioEQDef?) {
        _tracks.update { tracks ->
            tracks.map { track ->
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id == clipId) clip.copy(audioEQ = eq) else clip
                })
            }
        }
    }

    fun createGroup(name: String, clipIds: List<String>): ClipGroup {
        val clipOrder = getAllClips()
            .sortedWith(compareBy<EditorClip> { it.startOffsetUs }.thenBy { it.endOffsetUs })
            .map { it.id }
        val orderedIds = clipIds.distinct().sortedBy { clipOrder.indexOf(it).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE }
        val group = ClipGroup(id = "group_${System.currentTimeMillis()}", clipIds = orderedIds, name = name)
        _groups.value = _groups.value + group
        return group
    }

    fun deleteGroup(groupId: String) {
        _groups.value = _groups.value.filter { it.id != groupId }
    }

    fun toggleGroupExpand(groupId: String) {
        _groups.value = _groups.value.map {
            if (it.id == groupId) it.copy(isExpanded = !it.isExpanded) else it
        }
    }

    fun addAdjustmentTrack() {
        val idx = _tracks.value.size
        _tracks.value = _tracks.value + Track(
            id = "adjustment_$idx", type = TrackType.ADJUSTMENT, label = "Adjustment $idx"
        )
        _selectedTrackIndex.value = _tracks.value.lastIndex.coerceAtLeast(0)
    }

    fun toggleMultiSelect(clipId: String) {
        val current = _selectedClipIds.value.toMutableSet()
        if (current.contains(clipId)) current.remove(clipId) else current.add(clipId)
        _selectedClipIds.value = current
        _selectedClipId.value = when (current.size) {
            0 -> null
            1 -> current.first()
            else -> _selectedClipId.value?.takeIf { it in current } ?: clipId
        }
    }

    fun clearSelection() {
        _selectedClipIds.value = emptySet()
        _selectedClipId.value = null
    }

    fun snapshot(): TimelineSnapshot = TimelineSnapshot(
        tracks = _tracks.value,
        groups = _groups.value,
        currentTimeUs = _currentTimeUs.value,
        selectedClipId = _selectedClipId.value,
        selectedClipIds = _selectedClipIds.value,
        selectedTrackIndex = _selectedTrackIndex.value,
        zoomLevel = _zoomLevel.value,
        snapEnabled = _snapEnabled.value
    )

    fun restoreSnapshot(snapshot: TimelineSnapshot) {
        _tracks.value = snapshot.tracks.ifEmpty {
            listOf(
                Track(id = "video_0", type = TrackType.VIDEO, label = "Video"),
                Track(id = "audio_0", type = TrackType.AUDIO, label = "Audio"),
                Track(id = "text_0", type = TrackType.TEXT, label = "Text"),
                Track(id = "overlay_0", type = TrackType.OVERLAY, label = "Overlay")
            )
        }
        _groups.value = snapshot.groups
        _currentTimeUs.value = snapshot.currentTimeUs.coerceAtLeast(0L)
        _selectedClipId.value = snapshot.selectedClipId
        _selectedClipIds.value = snapshot.selectedClipIds
        _selectedTrackIndex.value = snapshot.selectedTrackIndex.coerceIn(0, (_tracks.value.lastIndex).coerceAtLeast(0))
        _zoomLevel.value = snapshot.zoomLevel.coerceIn(0.1f, 10f)
        _snapEnabled.value = snapshot.snapEnabled
        recalculateDuration()
    }

    fun setSelection(primaryClipId: String?, selectedClipIds: Set<String>) {
        _selectedClipId.value = primaryClipId
        _selectedClipIds.value = selectedClipIds
    }

    fun replaceAllTracks(tracks: List<Track>) {
        _tracks.value = tracks.ifEmpty {
            listOf(
                Track(id = "video_0", type = TrackType.VIDEO, label = "Video"),
                Track(id = "audio_0", type = TrackType.AUDIO, label = "Audio"),
                Track(id = "text_0", type = TrackType.TEXT, label = "Text"),
                Track(id = "overlay_0", type = TrackType.OVERLAY, label = "Overlay")
            )
        }
        _currentTimeUs.value = 0L
        _selectedClipId.value = null
        _selectedClipIds.value = emptySet()
        _groups.value = emptyList()
        _selectedTrackIndex.value = 0
        recalculateDuration()
    }

    fun replaceState(tracks: List<Track>, groups: List<ClipGroup>) {
        replaceState(tracks = tracks, groups = groups, zoomLevel = 1f, snapEnabled = true)
    }

    fun replaceState(
        tracks: List<Track>,
        groups: List<ClipGroup>,
        zoomLevel: Float,
        snapEnabled: Boolean
    ) {
        _tracks.value = tracks.ifEmpty {
            listOf(
                Track(id = "video_0", type = TrackType.VIDEO, label = "Video"),
                Track(id = "audio_0", type = TrackType.AUDIO, label = "Audio"),
                Track(id = "text_0", type = TrackType.TEXT, label = "Text"),
                Track(id = "overlay_0", type = TrackType.OVERLAY, label = "Overlay")
            )
        }
        _groups.value = groups
        _currentTimeUs.value = 0L
        _selectedClipId.value = null
        _selectedClipIds.value = emptySet()
        _selectedTrackIndex.value = 0
        _snapEnabled.value = snapEnabled
        _zoomLevel.value = zoomLevel.coerceIn(0.1f, 10f)
        recalculateDuration()
    }

    fun deleteSelected() {
        val toDelete = _selectedClipIds.value
        if (toDelete.isEmpty()) return
        _tracks.update { tracks ->
            tracks.map { track ->
                track.copy(clips = track.clips.filter { it.id !in toDelete })
            }
        }
        if (_selectedClipId.value in toDelete) {
            _selectedClipId.value = null
        }
        _selectedClipIds.value = emptySet()
        _groups.value = _groups.value.mapNotNull { group ->
            val remainingIds = group.clipIds.filter { it !in toDelete }
            when {
                remainingIds.isEmpty() -> null
                else -> group.copy(clipIds = remainingIds)
            }
        }
        recalculateDuration()
    }

    fun rippleDeleteSelection() {
        val selectedIds = _selectedClipIds.value.ifEmpty {
            _selectedClipId.value?.let { setOf(it) } ?: emptySet()
        }
        if (selectedIds.isEmpty()) return

        val selectedClips = getAllClips().filter { it.id in selectedIds }
        if (selectedClips.isEmpty()) return
        val deleteStartUs = selectedClips.minOf { it.startOffsetUs }
        val deleteEndUs = selectedClips.maxOf { it.endOffsetUs }
        val shiftUs = (deleteEndUs - deleteStartUs).coerceAtLeast(0L)

        _tracks.update { tracks ->
            tracks.map { track ->
                val remainingClips = track.clips
                    .filter { it.id !in selectedIds }
                    .map { clip ->
                        if (clip.startOffsetUs >= deleteEndUs) {
                            clip.copy(
                                startOffsetUs = (clip.startOffsetUs - shiftUs).coerceAtLeast(0L),
                                endOffsetUs = (clip.endOffsetUs - shiftUs).coerceAtLeast(0L)
                            )
                        } else {
                            clip
                        }
                    }
                track.copy(clips = remainingClips)
            }
        }

        if (_selectedClipId.value in selectedIds) {
            _selectedClipId.value = null
        }
        _selectedClipIds.value = emptySet()
        _groups.value = _groups.value.mapNotNull { group ->
            val remainingIds = group.clipIds.filter { it !in selectedIds }
            when {
                remainingIds.isEmpty() -> null
                else -> group.copy(clipIds = remainingIds)
            }
        }
        recalculateDuration()
    }

    fun toggleSnap() {
        _snapEnabled.value = !_snapEnabled.value
    }

    fun clear() {
        _tracks.value = listOf(
            Track(id = "video_0", type = TrackType.VIDEO, label = "Video"),
            Track(id = "audio_0", type = TrackType.AUDIO, label = "Audio"),
            Track(id = "text_0", type = TrackType.TEXT, label = "Text"),
            Track(id = "overlay_0", type = TrackType.OVERLAY, label = "Overlay")
        )
        _currentTimeUs.value = 0L
        _totalDurationUs.value = 0L
        _selectedClipId.value = null
        _selectedClipIds.value = emptySet()
        _groups.value = emptyList()
        _selectedTrackIndex.value = 0
        _zoomLevel.value = 1f
        _snapEnabled.value = true
    }

    fun getAllClips(): List<EditorClip> = _tracks.value.flatMap { it.clips }

    fun getClip(clipId: String): EditorClip? = getAllClips().find { it.id == clipId }

    private fun effectiveTrimEndUs(clip: EditorClip): Long {
        if (clip.trimEndUs > clip.trimStartUs) return clip.trimEndUs
        val sourceDurationUs = ((clip.endOffsetUs - clip.startOffsetUs).coerceAtLeast(0L) * clip.speed.coerceAtLeast(0.01f)).toLong()
        return clip.trimStartUs.coerceAtLeast(0L) + sourceDurationUs
    }

    private fun snapTimeUs(trackIndex: Int, clipId: String, candidateStartUs: Long): Long {
        val track = _tracks.value.getOrNull(trackIndex) ?: return candidateStartUs
        val clip = track.clips.find { it.id == clipId } ?: return candidateStartUs
        val durationUs = clip.durationUs.coerceAtLeast(0L)
        val candidateEndUs = candidateStartUs + durationUs
        val guides = buildList {
            add(0L)
            addAll(track.clips.filter { it.id != clipId }.flatMap { listOf(it.startOffsetUs, it.endOffsetUs) })
        }
        val snapThresholdUs = 120_000L
        val bestStartGuide = guides.minByOrNull { abs(it - candidateStartUs) }
        val bestEndGuide = guides.minByOrNull { abs(it - candidateEndUs) }

        val startDelta = bestStartGuide?.let { it - candidateStartUs }
        val endDelta = bestEndGuide?.let { it - candidateEndUs }
        val startDistance = startDelta?.let(::abs) ?: Long.MAX_VALUE
        val endDistance = endDelta?.let(::abs) ?: Long.MAX_VALUE

        return when {
            startDistance <= endDistance && startDistance <= snapThresholdUs -> candidateStartUs + (startDelta ?: 0L)
            endDistance < startDistance && endDistance <= snapThresholdUs -> candidateStartUs + (endDelta ?: 0L)
            else -> candidateStartUs
        }.coerceAtLeast(0L)
    }

    private fun recalculateDuration() {
        _totalDurationUs.value = _tracks.value
            .filter {
                it.type == TrackType.VIDEO ||
                        it.type == TrackType.OVERLAY ||
                        it.type == TrackType.TEXT ||
                        it.type == TrackType.STICKER ||
                        it.type == TrackType.ADJUSTMENT
            }
            .flatMap { it.clips }
            .maxOfOrNull { it.endOffsetUs } ?: 0L
    }
}
