package com.changecut.feature.editor.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.changecut.core.ai.AiBestMomentPicker
import com.changecut.core.ai.AiSceneDetection
import com.changecut.core.ai.SceneChangeResult
import com.changecut.core.ai.ScoredClip
import com.changecut.core.editor.EditorClip
import com.changecut.core.editor.SnapshotCommand
import com.changecut.core.editor.TrackManager
import com.changecut.core.editor.TransitionDef
import com.changecut.core.editor.UndoRedoManager
import com.changecut.core.gpu.FrameScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class EditStyle {
    CINEMATIC, FAST_PACED, VLOG, SLIDESHOW, CUSTOM
}

enum class AnalysisStep {
    IDLE, ANALYZING, SCORING, BUILDING, COMPLETE, ERROR
}

data class AutoEditResult(
    val clips: List<EditorClip> = emptyList(),
    val totalDurationMs: Long = 0L,
    val transitions: List<TransitionDef> = emptyList(),
    val musicPath: String? = null,
    val style: EditStyle = EditStyle.CINEMATIC
)

data class AiAutoEditUiState(
    val selectedClipPaths: List<String> = emptyList(),
    val analyzedClips: List<ScoredClip> = emptyList(),
    val sceneChanges: List<SceneChangeResult> = emptyList(),
    val analysisProgress: Float = 0f,
    val analysisStep: AnalysisStep = AnalysisStep.IDLE,
    val autoEditResult: AutoEditResult? = null,
    val targetDurationMs: Long = 30_000L,
    val selectedStyle: EditStyle = EditStyle.CINEMATIC,
    val isApplying: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AiAutoEditViewModel @Inject constructor(
    private val sceneDetection: AiSceneDetection,
    private val bestMomentPicker: AiBestMomentPicker,
    private val trackManager: TrackManager,
    private val undoRedoManager: UndoRedoManager,
    private val frameScheduler: FrameScheduler
) : ViewModel() {

    private val _state = MutableStateFlow(AiAutoEditUiState())
    val state: StateFlow<AiAutoEditUiState> = _state.asStateFlow()

    fun setClips(clipPaths: List<String>) {
        _state.update { it.copy(selectedClipPaths = clipPaths) }
    }

    fun setTargetDuration(durationMs: Long) {
        _state.update { it.copy(targetDurationMs = durationMs) }
    }

    fun setStyle(style: EditStyle) {
        _state.update { it.copy(selectedStyle = style) }
    }

    fun analyzeClips() {
        val paths = _state.value.selectedClipPaths
        if (paths.isEmpty()) {
            _state.update { it.copy(error = "No clips selected for analysis") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(
                analysisStep = AnalysisStep.ANALYZING,
                analysisProgress = 0f,
                error = null
            ) }

            try {
                val totalClips = paths.size
                val allSceneChanges = mutableListOf<SceneChangeResult>()
                var processed = 0

                val sceneResults = paths.flatMap { path ->
                    val changes = sceneDetection.detectSceneChanges(path)
                    processed++
                    _state.update { it.copy(
                        analysisProgress = (processed.toFloat() / totalClips) * 0.5f
                    ) }
                    allSceneChanges.addAll(changes)
                    changes
                }

                _state.update { it.copy(
                    analysisStep = AnalysisStep.SCORING,
                    analysisProgress = 0.5f
                ) }

                val scoredClips = bestMomentPicker.analyzeClips(paths)

                _state.update { it.copy(
                    analysisStep = AnalysisStep.COMPLETE,
                    analysisProgress = 1f,
                    analyzedClips = scoredClips,
                    sceneChanges = allSceneChanges
                ) }
            } catch (e: Exception) {
                _state.update { it.copy(
                    analysisStep = AnalysisStep.ERROR,
                    error = e.message ?: "Analysis failed"
                ) }
            }
        }
    }

    fun generateAutoEdit() {
        val currentState = _state.value
        val scoredClips = currentState.analyzedClips
        if (scoredClips.isEmpty()) {
            _state.update { it.copy(error = "No analyzed clips to build timeline") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(
                analysisStep = AnalysisStep.BUILDING,
                analysisProgress = 0f
            ) }

            try {
                val introClip = bestMomentPicker.pickIntro(scoredClips)
                val durationPerStyle = calculateStyleDuration(
                    currentState.targetDurationMs,
                    currentState.selectedStyle
                )
                val bestMoments = bestMomentPicker.pickBestMoments(
                    scoredClips,
                    durationPerStyle
                )

                val transitionList = buildTransitions(currentState.selectedStyle)

                val editorClips = mutableListOf<EditorClip>()
                var timelineCursorUs = 0L

                if (introClip != null && introClip.durationUs > 0) {
                    val introDurationUs = (introClip.durationUs * 0.7).toLong()
                    editorClips.add(
                        EditorClip(
                            id = UUID.randomUUID().toString(),
                            sourceUri = introClip.videoPath,
                            label = "Intro",
                            startOffsetUs = timelineCursorUs,
                            endOffsetUs = timelineCursorUs + introDurationUs,
                            trimStartUs = introClip.startTimeUs,
                            trimEndUs = introClip.startTimeUs + introDurationUs
                        )
                    )
                    timelineCursorUs += introDurationUs
                }

                for ((index, clip) in bestMoments.withIndex()) {
                    val transitionIn = if (editorClips.isNotEmpty()) {
                        transitionList.getOrNull((index - 1).coerceAtLeast(0))
                    } else {
                        null
                    }
                    editorClips.add(
                        EditorClip(
                            id = UUID.randomUUID().toString(),
                            sourceUri = clip.videoPath,
                            label = "Highlight ${editorClips.size + 1}",
                            startOffsetUs = timelineCursorUs,
                            endOffsetUs = timelineCursorUs + clip.durationUs,
                            trimStartUs = clip.startTimeUs,
                            trimEndUs = clip.startTimeUs + clip.durationUs,
                            transitionIn = transitionIn,
                            transitionOut = transitionList.getOrNull(index)
                        )
                    )
                    timelineCursorUs += clip.durationUs
                }

                val totalDuration = editorClips.sumOf { it.durationUs }

                val result = AutoEditResult(
                    clips = editorClips,
                    totalDurationMs = totalDuration / 1000L,
                    transitions = transitionList,
                    style = currentState.selectedStyle
                )

                _state.update { it.copy(
                    analysisStep = AnalysisStep.COMPLETE,
                    analysisProgress = 1f,
                    autoEditResult = result
                ) }
            } catch (e: Exception) {
                _state.update { it.copy(
                    analysisStep = AnalysisStep.ERROR,
                    error = e.message ?: "Auto edit generation failed"
                ) }
            }
        }
    }

    fun applyAutoEdit() {
        val result = _state.value.autoEditResult ?: return
        _state.update { it.copy(isApplying = true) }

        try {
            undoRedoManager.execute(
                SnapshotCommand(trackManager, "Apply AI auto edit") {
                    trackManager.clear()
                    for ((i, clip) in result.clips.withIndex()) {
                        trackManager.addClip(0, clip)
                        if (i < result.transitions.size) {
                            val transition = result.transitions[i]
                            trackManager.applyTransition(clip.id, clip.transitionIn, transition)
                        }
                    }
                    result.clips.firstOrNull()?.let { firstClip ->
                        trackManager.selectTrack(0)
                        trackManager.selectClip(firstClip.id)
                    }
                }
            )
            frameScheduler.pause()
            frameScheduler.seekTo(0L)
            _state.update { it.copy(isApplying = false) }
        } catch (e: Exception) {
            _state.update { it.copy(
                isApplying = false,
                error = e.message ?: "Failed to apply auto edit"
            ) }
        }
    }

    fun generateIntro() {
        val scoredClips = _state.value.analyzedClips
        val intro = bestMomentPicker.pickIntro(scoredClips)
        if (intro != null) {
            val introDurationUs = intro.durationUs
            val introClip = EditorClip(
                id = UUID.randomUUID().toString(),
                sourceUri = intro.videoPath,
                label = "AI Intro",
                startOffsetUs = 0L,
                endOffsetUs = introDurationUs,
                trimStartUs = intro.startTimeUs,
                trimEndUs = intro.startTimeUs + intro.durationUs
            )
            undoRedoManager.execute(
                SnapshotCommand(trackManager, "Generate AI intro") {
                    trackManager.addClip(0, introClip)
                    trackManager.selectTrack(0)
                    trackManager.selectClip(introClip.id)
                }
            )
        }
    }

    fun generateHighlightReel() {
        val scoredClips = _state.value.analyzedClips
        val best = bestMomentPicker.pickBestMoments(scoredClips, 60_000_000L)
        val existing = trackManager.getAllClips().size

        var timelineCursorUs = trackManager.totalDurationUs.value
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Generate highlight reel") {
                var firstNewClipId: String? = null
                for ((i, clip) in best.withIndex()) {
                    val editorClip = EditorClip(
                        id = UUID.randomUUID().toString(),
                        sourceUri = clip.videoPath,
                        label = "Highlight ${existing + i + 1}",
                        startOffsetUs = timelineCursorUs,
                        endOffsetUs = timelineCursorUs + clip.durationUs,
                        trimStartUs = clip.startTimeUs,
                        trimEndUs = clip.startTimeUs + clip.durationUs
                    )
                    if (firstNewClipId == null) {
                        firstNewClipId = editorClip.id
                    }
                    trackManager.addClip(0, editorClip)
                    timelineCursorUs += clip.durationUs
                }
                firstNewClipId?.let { clipId ->
                    trackManager.selectTrack(0)
                    trackManager.selectClip(clipId)
                }
            }
        )
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun reset() {
        _state.value = AiAutoEditUiState()
    }

    private fun calculateStyleDuration(baseDurationMs: Long, style: EditStyle): Long {
        return when (style) {
            EditStyle.FAST_PACED -> (baseDurationMs * 0.6).toLong()
            EditStyle.SLIDESHOW -> (baseDurationMs * 1.3).toLong()
            else -> baseDurationMs
        } * 1000L
    }

    private fun buildTransitions(style: EditStyle): List<TransitionDef> {
        return when (style) {
            EditStyle.CINEMATIC -> listOf(
                TransitionDef(id = "fade_600", type = "fade", durationMs = 600),
                TransitionDef(id = "fade_400", type = "fade", durationMs = 400),
                TransitionDef(id = "slide_500", type = "slide", durationMs = 500)
            )
            EditStyle.FAST_PACED -> listOf(
                TransitionDef(id = "flash_200", type = "flash", durationMs = 200),
                TransitionDef(id = "slide_200", type = "slide", durationMs = 200),
                TransitionDef(id = "flash_150", type = "flash", durationMs = 150)
            )
            EditStyle.VLOG -> listOf(
                TransitionDef(id = "fade_300", type = "fade", durationMs = 300),
                TransitionDef(id = "slide_300", type = "slide", durationMs = 300)
            )
            EditStyle.SLIDESHOW -> listOf(
                TransitionDef(id = "zoom_500", type = "zoom", durationMs = 500),
                TransitionDef(id = "fade_500", type = "fade", durationMs = 500),
                TransitionDef(id = "slide_400", type = "slide", durationMs = 400)
            )
            EditStyle.CUSTOM -> listOf(
                TransitionDef(id = "fade_300", type = "fade", durationMs = 300)
            )
        }
    }
}
