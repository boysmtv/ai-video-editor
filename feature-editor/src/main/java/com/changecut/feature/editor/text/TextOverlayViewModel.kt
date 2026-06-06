package com.changecut.feature.editor.text

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.changecut.core.editor.TrackType
import com.changecut.core.editor.SnapshotCommand
import com.changecut.core.editor.TextClipStyle
import com.changecut.core.editor.TrackManager
import com.changecut.core.editor.UndoRedoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class TextOverlayUiState(
    val textContent: String = "",
    val fontFamily: String = "Default",
    val fontSize: Float = 24f,
    val color: Long = 0xFFFFFFFF,
    val alignment: Int = 1,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val shadow: Boolean = false,
    val outline: Boolean = false,
    val backgroundColor: Long? = null,
    val outlineColor: Long = 0xFF000000,
    val animationIn: String? = null,
    val animationOut: String? = null,
    val previewText: String = "Preview Text"
)

@HiltViewModel
class TextOverlayViewModel @Inject constructor(
    private val trackManager: TrackManager,
    private val undoRedoManager: UndoRedoManager
) : ViewModel() {

    private val _state = MutableStateFlow(TextOverlayUiState())
    val state: StateFlow<TextOverlayUiState> = _state.asStateFlow()
    private var boundClipId: String? = null
    private var lastSyncedSignature: String? = null
    private var isDirty: Boolean = false

    init {
        viewModelScope.launch {
            trackManager.tracks.collect {
                syncBoundClipFromTimeline()
            }
        }
    }

    fun setTextContent(text: String) {
        isDirty = true
        _state.update { it.copy(textContent = text, previewText = text.ifBlank { "Preview Text" }) }
    }

    fun setFontFamily(font: String) {
        isDirty = true
        _state.update { it.copy(fontFamily = font) }
    }

    fun setFontSize(size: Float) {
        isDirty = true
        _state.update { it.copy(fontSize = size.coerceIn(8f, 120f)) }
    }

    fun setColor(color: Long) {
        isDirty = true
        _state.update { it.copy(color = color) }
    }

    fun setAlignment(alignment: Int) {
        isDirty = true
        _state.update { it.copy(alignment = alignment.coerceIn(0, 4)) }
    }

    fun toggleBold() {
        isDirty = true
        _state.update { it.copy(bold = !it.bold) }
    }

    fun toggleItalic() {
        isDirty = true
        _state.update { it.copy(italic = !it.italic) }
    }

    fun toggleShadow() {
        isDirty = true
        _state.update { it.copy(shadow = !it.shadow) }
    }

    fun toggleOutline() {
        isDirty = true
        _state.update { it.copy(outline = !it.outline) }
    }

    fun setBackgroundColor(color: Long?) {
        isDirty = true
        _state.update { it.copy(backgroundColor = color) }
    }

    fun setOutlineColor(color: Long) {
        isDirty = true
        _state.update { it.copy(outlineColor = color) }
    }

    fun setAnimationIn(anim: String?) {
        isDirty = true
        _state.update { it.copy(animationIn = anim) }
    }

    fun setAnimationOut(anim: String?) {
        isDirty = true
        _state.update { it.copy(animationOut = anim) }
    }

    fun bindEditor(clipId: String?, initialText: String = "", initialStyle: TextClipStyle? = null) {
        boundClipId = clipId
        isDirty = false
        val clip = clipId?.let(trackManager::getClip)
        when {
            clip != null -> applyClipToState(clip.textStyle, clip.textContent.orEmpty())
            clipId != null && initialStyle != null -> applyClipToState(initialStyle, initialText)
            clipId != null -> applyClipToState(null, initialText)
            else -> applyClipToState(null, "")
        }
    }

    fun applyTextStyle(): TextClipStyle {
        val s = _state.value
        return TextClipStyle(
            fontName = s.fontFamily,
            fontSize = s.fontSize,
            color = s.color,
            backgroundColor = s.backgroundColor,
            alignment = s.alignment,
            bold = s.bold,
            italic = s.italic,
            shadow = s.shadow,
            outline = s.outline,
            outlineColor = s.outlineColor,
            animationIn = s.animationIn,
            animationOut = s.animationOut
        )
    }

    fun saveToTrack(trackIndex: Int, clipId: String? = null) {
        val s = _state.value
        if (s.textContent.isBlank()) return
        viewModelScope.launch {
            val style = applyTextStyle()
            undoRedoManager.execute(
                SnapshotCommand(trackManager, "Save text overlay") {
                    val tracks = trackManager.tracks.value
                    var resolvedTrackIndex = when {
                        trackIndex in tracks.indices && tracks[trackIndex].type == TrackType.TEXT -> trackIndex
                        else -> tracks.indexOfFirst { it.type == TrackType.TEXT }
                    }
                    if (resolvedTrackIndex < 0) {
                        trackManager.addTrack(TrackType.TEXT, "Text")
                        resolvedTrackIndex = trackManager.tracks.value.indexOfFirst { it.type == TrackType.TEXT }
                    }
                    if (resolvedTrackIndex < 0) return@SnapshotCommand
                    val track = trackManager.tracks.value[resolvedTrackIndex]
                    if (clipId != null) {
                        val existingClip = track.clips.find { it.id == clipId }
                        if (existingClip != null) {
                            val updated = existingClip.copy(
                                textContent = s.textContent,
                                textStyle = style,
                                contentRole = existingClip.contentRole ?: "text"
                            )
                            trackManager.updateClip(resolvedTrackIndex, updated)
                            trackManager.selectTrack(resolvedTrackIndex)
                            trackManager.selectClip(updated.id)
                            boundClipId = updated.id
                            lastSyncedSignature = buildSignature(updated.textContent.orEmpty(), updated.textStyle)
                            isDirty = false
                            return@SnapshotCommand
                        }
                    }
                    val currentTimeUs = trackManager.currentTimeUs.value
                    val newClip = com.changecut.core.editor.EditorClip(
                        id = UUID.randomUUID().toString(),
                        sourceUri = "",
                        label = "Text: ${s.textContent.take(20)}",
                        contentRole = "text",
                        startOffsetUs = currentTimeUs,
                        endOffsetUs = currentTimeUs + 5_000_000,
                        textContent = s.textContent,
                        textStyle = style
                    )
                    trackManager.addClip(resolvedTrackIndex, newClip)
                    trackManager.selectTrack(resolvedTrackIndex)
                    trackManager.selectClip(newClip.id)
                    boundClipId = newClip.id
                    lastSyncedSignature = buildSignature(newClip.textContent.orEmpty(), newClip.textStyle)
                    isDirty = false
                }
            )
        }
    }

    fun reset() {
        boundClipId = null
        applyClipToState(null, "")
    }

    fun loadFromStyle(style: TextClipStyle, text: String) {
        applyClipToState(style, text)
    }

    private fun syncBoundClipFromTimeline() {
        val clipId = boundClipId ?: return
        val clip = trackManager.getClip(clipId) ?: return
        val nextSignature = buildSignature(clip.textContent.orEmpty(), clip.textStyle)
        val currentSignature = buildSignature(_state.value.textContent, applyTextStyle())
        val shouldSync = !isDirty || currentSignature == lastSyncedSignature
        if (nextSignature != lastSyncedSignature && shouldSync) {
            applyClipToState(clip.textStyle, clip.textContent.orEmpty())
        }
    }

    private fun applyClipToState(style: TextClipStyle?, text: String) {
        val resolvedStyle = style ?: TextClipStyle()
        _state.value = TextOverlayUiState(
            textContent = text,
            previewText = text.ifBlank { "Preview Text" },
            fontFamily = resolvedStyle.fontName,
            fontSize = resolvedStyle.fontSize,
            color = resolvedStyle.color,
            alignment = resolvedStyle.alignment,
            bold = resolvedStyle.bold,
            italic = resolvedStyle.italic,
            shadow = resolvedStyle.shadow,
            outline = resolvedStyle.outline,
            backgroundColor = resolvedStyle.backgroundColor,
            outlineColor = resolvedStyle.outlineColor,
            animationIn = resolvedStyle.animationIn,
            animationOut = resolvedStyle.animationOut
        )
        lastSyncedSignature = buildSignature(text, resolvedStyle)
        isDirty = false
    }

    private fun buildSignature(text: String, style: TextClipStyle?): String {
        val resolvedStyle = style ?: TextClipStyle()
        return listOf(
            text,
            resolvedStyle.fontName,
            resolvedStyle.fontSize.toString(),
            resolvedStyle.color.toString(),
            resolvedStyle.backgroundColor?.toString().orEmpty(),
            resolvedStyle.alignment.toString(),
            resolvedStyle.bold.toString(),
            resolvedStyle.italic.toString(),
            resolvedStyle.shadow.toString(),
            resolvedStyle.outline.toString(),
            resolvedStyle.outlineColor.toString(),
            resolvedStyle.animationIn.orEmpty(),
            resolvedStyle.animationOut.orEmpty()
        ).joinToString("|")
    }
}
