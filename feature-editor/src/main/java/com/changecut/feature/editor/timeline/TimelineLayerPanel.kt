package com.changecut.feature.editor.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.changecut.core.editor.Track
import com.changecut.core.editor.TrackType

@Composable
fun TimelineLayerPanel(
    tracks: List<Track>,
    selectedTrackIndex: Int,
    selectedClipIds: Set<String>,
    onTrackSelected: (Int) -> Unit,
    onToggleVisibility: (Int) -> Unit,
    onToggleMute: (Int) -> Unit,
    onToggleLock: (Int) -> Unit,
    onSoloTrack: (Int) -> Unit,
    onShowAllTracks: () -> Unit,
    onTrackSelectAll: (Int) -> Unit
) {
    val isSoloActive = tracks.count { it.isVisible } == 1
    Column(
        modifier = Modifier
            .width(140.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Text(
            text = "Layers",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(8.dp)
        )
        Text(
            text = "Tap select, hold select all",
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        LazyColumn {
            itemsIndexed(tracks) { index, track ->
                LayerItem(
                    track = track,
                    isSelected = index == selectedTrackIndex,
                    isSoloActive = isSoloActive,
                    selectedClipCount = track.clips.count { it.id in selectedClipIds },
                    trackIcon = when (track.type) {
                        TrackType.VIDEO -> Icons.Default.VideoLibrary
                        TrackType.AUDIO -> Icons.Default.MusicNote
                        TrackType.TEXT -> Icons.Default.Subtitles
                        TrackType.OVERLAY -> Icons.Default.PhotoLibrary
                        TrackType.STICKER -> Icons.Default.PhotoLibrary
                        TrackType.ADJUSTMENT -> Icons.Default.Tune
                        else -> Icons.Default.VideoLibrary
                    },
                    onClick = { onTrackSelected(index) },
                    onToggleVisibility = { onToggleVisibility(index) },
                    onToggleMute = { onToggleMute(index) },
                    onToggleLock = { onToggleLock(index) },
                    onSolo = { onSoloTrack(index) },
                    onShowAll = onShowAllTracks,
                    onLongPress = { onTrackSelectAll(index) }
                )
            }
        }
    }
}

@Composable
private fun LayerItem(
    track: Track,
    isSelected: Boolean,
    isSoloActive: Boolean,
    selectedClipCount: Int,
    trackIcon: ImageVector,
    onClick: () -> Unit,
    onToggleVisibility: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleLock: () -> Unit,
    onSolo: () -> Unit,
    onShowAll: () -> Unit,
    onLongPress: () -> Unit
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    else Color.Transparent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() }
                )
            }
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = trackIcon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = buildString {
                    append("${track.label} (${track.clips.size})")
                    if (selectedClipCount > 0) append(" [$selectedClipCount]")
                },
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            SmallIconButton(
                icon = if (track.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                onClick = onToggleVisibility
            )
            if (track.type == TrackType.AUDIO || track.type == TrackType.VIDEO) {
                SmallIconButton(
                    icon = if (track.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    onClick = onToggleMute
                )
            }
            SmallIconButton(
                icon = Icons.Default.CenterFocusStrong,
                onClick = if (isSoloActive && track.isVisible) onShowAll else onSolo
            )
            SmallIconButton(
                icon = if (track.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                onClick = onToggleLock
            )
        }
    }
}

@Composable
private fun SmallIconButton(icon: ImageVector, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(20.dp)) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(12.dp))
    }
}
