package com.changecut.feature.editor.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.changecut.core.editor.EditorClip
import com.changecut.core.editor.Track
import com.changecut.core.editor.TrackType

private const val BASE_PIXELS_PER_SECOND = 50f
private val TRACK_HEIGHT = 52.dp
private val TRACK_LABEL_WIDTH = 40.dp
private val PLAYHEAD_COLOR = Color(0xFF6C63FF)
private val PLAYHEAD_WIDTH = 2f

private val TRACK_COLORS = mapOf(
    TrackType.VIDEO to Color(0xFF2196F3),
    TrackType.AUDIO to Color(0xFF4CAF50),
    TrackType.TEXT to Color(0xFFFF9800),
    TrackType.OVERLAY to Color(0xFF9C27B0),
    TrackType.EFFECT to Color(0xFFE91E63),
    TrackType.STICKER to Color(0xFF00BCD4),
    TrackType.ADJUSTMENT to Color(0xFFFFC107)
)

private fun clipColor(type: TrackType): Color = TRACK_COLORS[type] ?: Color(0xFF607D8B)

@Composable
fun TimelineView(
    tracks: List<Track>,
    currentTimeMs: Long,
    durationMs: Long,
    selectedTrackIndex: Int,
    selectedClipId: String?,
    selectedClipIds: Set<String>,
    snapEnabled: Boolean,
    zoomLevel: Float,
    onTimeChange: (Long) -> Unit,
    onClipSelected: (clipId: String?, trackIndex: Int) -> Unit,
    onClipMultiToggle: (clipId: String, trackIndex: Int) -> Unit,
    onTrackSelected: (Int) -> Unit,
    onPlayPause: () -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val horizontalScrollState = rememberScrollState()
    val pixelsPerSecond = BASE_PIXELS_PER_SECOND * zoomLevel
    val totalWidthPx = (durationMs / 1000f) * pixelsPerSecond
    val trackHeightPx = with(density) { TRACK_HEIGHT.toPx() }
    val viewportWidthPx = with(density) { 320.dp.toPx() }

    LaunchedEffect(currentTimeMs, zoomLevel) {
        val playheadPx = (currentTimeMs / 1000f) * pixelsPerSecond
        val target = (playheadPx - (viewportWidthPx * 0.35f)).toInt()
            .coerceIn(0, horizontalScrollState.maxValue)
        if (kotlin.math.abs(horizontalScrollState.value - target) > 48) {
            horizontalScrollState.scrollTo(target)
        }
    }

    Column(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPlayPause) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }
            Text(
                text = formatTime(currentTimeMs),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp)
            )
            Text(
                text = "/ ${formatTime(durationMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
            val selectionCount = selectedClipIds.size.takeIf { it > 0 }
                ?: if (selectedClipId != null) 1 else 0
            Text(
                text = "Sel $selectionCount | ${if (snapEnabled) "Snap" else "Free"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 10.dp)
            )
            if (selectionCount > 1) {
                Text(
                    text = "Multi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(tracks) { index, track ->
                TimelineTrackRow(
                    track = track,
                    trackIndex = index,
                    isSelected = index == selectedTrackIndex,
                    totalWidthPx = totalWidthPx,
                    pixelsPerSecond = pixelsPerSecond,
                    trackHeightPx = trackHeightPx,
                    currentTimeMs = currentTimeMs,
                    selectedClipId = selectedClipId,
                    selectedClipIds = selectedClipIds,
                    zoomLevel = zoomLevel,
                    horizontalScrollState = horizontalScrollState,
                    onTimeChange = onTimeChange,
                    onClipSelected = { clipId -> onClipSelected(clipId, index) },
                    onClipMultiToggle = { clipId -> onClipMultiToggle(clipId, index) },
                    onTrackSelected = { onTrackSelected(index) }
                )
            }
        }
    }
}

@Composable
private fun TimelineTrackRow(
    track: Track,
    trackIndex: Int,
    isSelected: Boolean,
    totalWidthPx: Float,
    pixelsPerSecond: Float,
    trackHeightPx: Float,
    currentTimeMs: Long,
    selectedClipId: String?,
    selectedClipIds: Set<String>,
    zoomLevel: Float,
    horizontalScrollState: ScrollState,
    onTimeChange: (Long) -> Unit,
    onClipSelected: (String?) -> Unit,
    onClipMultiToggle: (String) -> Unit,
    onTrackSelected: () -> Unit
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        trackIndex % 2 == 0 -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TRACK_HEIGHT)
            .background(bgColor)
            .clickable(onClick = onTrackSelected)
    ) {
        Box(
            modifier = Modifier
                .width(TRACK_LABEL_WIDTH)
                .fillMaxHeight()
                .background(
                    if (isSelected) clipColor(track.type).copy(alpha = 0.2f)
                    else Color.Transparent
                )
                .clickable(onClick = onTrackSelected),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = track.label.take(3),
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = clipColor(track.type),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(horizontalScrollState)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            val contentX = offset.x + horizontalScrollState.value
                            val clip = findClipAtPosition(track.clips, contentX, pixelsPerSecond)
                            if (clip != null) {
                                onClipSelected(clip.id)
                            } else {
                                onClipSelected(null)
                                val timeMs = (contentX / pixelsPerSecond * 1000f).toLong()
                                onTimeChange(timeMs.coerceAtLeast(0))
                            }
                        },
onLongPress = { offset ->
                             val contentX = offset.x + horizontalScrollState.value
                             val clip = findClipAtPosition(track.clips, contentX, pixelsPerSecond)
                             if (clip != null) {
                                 onClipMultiToggle(clip.id)
                             }
                        }
                    )
                }
        ) {
            val clips = track.clips
            val visible = track.isVisible

            if (visible) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .width(with(density) { totalWidthPx.toDp() })
                ) {
                    val type = track.type
                    val baseColor = clipColor(type)

                    for (clip in clips) {
                        val clipStart = (clip.startOffsetUs / 1_000_000f) * pixelsPerSecond
                        val clipWidth = (clip.durationUs / 1_000_000f) * pixelsPerSecond
                        val isPrimarySelected = clip.id == selectedClipId
                        val isMultiSelected = clip.id in selectedClipIds

                        drawRect(
                            color = when {
                                isPrimarySelected -> baseColor.copy(alpha = 0.9f)
                                isMultiSelected -> baseColor.copy(alpha = 0.72f)
                                else -> baseColor.copy(alpha = 0.55f)
                            },
                            topLeft = Offset(clipStart, 4f),
                            size = androidx.compose.ui.geometry.Size(
                                clipWidth.coerceAtLeast(2f),
                                trackHeightPx - 8f
                            )
                        )

                        if (isMultiSelected) {
                            drawRect(
                                color = Color.White.copy(alpha = 0.9f),
                                topLeft = Offset(clipStart, 4f),
                                size = androidx.compose.ui.geometry.Size(
                                    clipWidth.coerceAtLeast(2f),
                                    3f
                                )
                            )
                        }

                        if (clipWidth > 30f) {
                            drawClipLabel(clip, clipStart, trackHeightPx, baseColor, textMeasurer)
                        }
                        if (clipWidth > 60f) {
                            drawClipMeta(clip, clipStart, clipWidth, trackHeightPx, textMeasurer)
                        }
                    }

                    val playheadX = (currentTimeMs / 1000f) * pixelsPerSecond
                    drawLine(
                        color = PLAYHEAD_COLOR,
                        start = Offset(playheadX, 0f),
                        end = Offset(playheadX, trackHeightPx),
                        strokeWidth = PLAYHEAD_WIDTH
                    )
                }
            }
        }
    }
}

private fun findClipAtPosition(clips: List<EditorClip>, x: Float, pixelsPerSecond: Float): EditorClip? {
    return clips.lastOrNull { clip ->
        val clipStart = (clip.startOffsetUs / 1_000_000f) * pixelsPerSecond
        val clipEnd = clipStart + (clip.durationUs / 1_000_000f) * pixelsPerSecond
        x in clipStart..clipEnd
    }
}

private fun DrawScope.drawClipLabel(clip: EditorClip, clipStart: Float, trackHeight: Float, color: Color, textMeasurer: TextMeasurer) {
    val labelText = clip.label
    val textLayoutResult = textMeasurer.measure(
        text = AnnotatedString(labelText),
        style = TextStyle(fontSize = 14.sp, color = Color.White)
    )
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(clipStart + 6f, trackHeight / 2f - textLayoutResult.size.height / 2f)
    )
}

private fun DrawScope.drawClipMeta(
    clip: EditorClip,
    clipStart: Float,
    clipWidth: Float,
    trackHeight: Float,
    textMeasurer: TextMeasurer
) {
    val parts = buildList {
        clip.contentRole?.takeIf { it.isNotBlank() }?.let { add(it.take(4).uppercase()) }
        if (clip.speed != 1f) add("${String.format("%.1f", clip.speed)}x")
        if (clip.transitionIn != null || clip.transitionOut != null) add("TR")
        if (clip.mask != null) add("MSK")
        if (clip.effectId != null) add("FX")
        if (clip.blendMode != com.changecut.core.editor.BlendMode.NORMAL) add("BLD")
        if (clip.audioEQ != null) add("EQ")
        if (clip.freezeDurationMs > 0) add("FRZ")
    }
    if (parts.isEmpty()) return
    val metaText = parts.joinToString(" ")
    val textLayoutResult = textMeasurer.measure(
        text = AnnotatedString(metaText),
        style = TextStyle(fontSize = 9.sp, color = Color.White.copy(alpha = 0.9f))
    )
    val metaY = trackHeight - textLayoutResult.size.height - 6f
    val maxX = clipStart + clipWidth - textLayoutResult.size.width - 4f
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset((clipStart + 6f).coerceAtMost(maxX), metaY.coerceAtLeast(4f))
    )
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return String.format("%02d:%02d", m, s)
}
