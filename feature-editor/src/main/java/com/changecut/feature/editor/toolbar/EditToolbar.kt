package com.changecut.feature.editor.toolbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EditToolbar(
    selectedClipId: String?,
    hasSelection: Boolean,
    selectedClipCount: Int,
    snapEnabled: Boolean,
    onAddVideo: () -> Unit,
    onAddImage: () -> Unit,
    onAddAudio: () -> Unit,
    onAddText: () -> Unit,
    onTemplates: () -> Unit,
    onSticker: () -> Unit,
    onMask: () -> Unit,
    onChromaKey: () -> Unit,
    onColor: () -> Unit,
    onSpeedRamp: () -> Unit,
    onEffects: () -> Unit,
    onTransition: () -> Unit,
    onKeyframe: () -> Unit,
    onAiAutoEdit: () -> Unit,
    onAiVoiceOver: () -> Unit,
    onAiCaption: () -> Unit,
    onSubtitle: () -> Unit,
    onAnimation: () -> Unit,
    onAudioEq: () -> Unit,
    onAdjustment: () -> Unit,
    onSelectTrack: () -> Unit,
    onToggleSnap: () -> Unit,
    onCopyStyle: () -> Unit,
    onPasteStyle: () -> Unit,
    onDuplicate: () -> Unit,
    onNudgeLeft: () -> Unit,
    onNudgeRight: () -> Unit,
    onSplit: () -> Unit,
    onMerge: () -> Unit,
    onDelete: () -> Unit,
    onRippleDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarButton(Icons.Default.VideoLibrary, "Video", onAddVideo)
                ToolbarButton(Icons.Default.PhotoLibrary, "Image", onAddImage)
                ToolbarButton(Icons.Default.MusicNote, "Audio", onAddAudio)
                ToolbarButton(Icons.Default.TextFields, "Text", onAddText)
                ToolbarButton(Icons.Default.PhotoLibrary, "Template", onTemplates)
                ToolbarButton(Icons.Default.AutoAwesome, "Sticker", onSticker)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarButton(Icons.Default.Layers, "Mask", onMask)
                ToolbarButton(Icons.Default.ColorLens, "Chroma", onChromaKey)
                ToolbarButton(Icons.Default.ColorLens, "Color", onColor)
                ToolbarButton(Icons.Default.SlowMotionVideo, "Speed", onSpeedRamp)
                ToolbarButton(Icons.Default.PushPin, if (snapEnabled) "Snap On" else "Snap Off", onToggleSnap)

                if (selectedClipCount > 0) {
                    ToolbarButton(Icons.Default.KeyboardArrowLeft, "Left", onNudgeLeft)
                    ToolbarButton(Icons.Default.KeyboardArrowRight, "Right", onNudgeRight)
                    if (selectedClipCount > 1) {
                        ToolbarButton(Icons.Default.ContentPaste, "Merge", onMerge)
                    } else {
                        ToolbarButton(Icons.Default.ContentCut, "Split", onSplit)
                    }
                } else if (hasSelection) {
                    ToolbarSpacer()
                    ToolbarSpacer()
                } else {
                    ToolbarSpacer()
                    ToolbarSpacer()
                    ToolbarSpacer()
                }

                if (hasSelection) {
                    ToolbarButton(Icons.Default.Delete, "Delete", onDelete)
                } else {
                    ToolbarSpacer()
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarButton(Icons.Default.Tune, "FX", onEffects)
                ToolbarButton(Icons.Default.Timeline, "Trans", onTransition)
                ToolbarButton(Icons.Default.AutoAwesome, "Key", onKeyframe)
                if (hasSelection) {
                    ToolbarButton(Icons.Default.ContentCopy, "Copy", onCopyStyle)
                    ToolbarButton(Icons.Default.ContentPaste, "Paste", onPasteStyle)
                    ToolbarButton(Icons.Default.PhotoLibrary, "Dup", onDuplicate)
                } else {
                    ToolbarButton(Icons.Default.AutoAwesome, "AI Edit", onAiAutoEdit)
                    ToolbarButton(Icons.Default.Mic, "AI VO", onAiVoiceOver)
                    ToolbarSpacer()
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarButton(Icons.Default.TextFields, "Caption", onAiCaption)
                ToolbarButton(Icons.Default.Subtitles, "Subtitle", onSubtitle)
                ToolbarButton(Icons.Default.AutoAwesome, "Anim", onAnimation)
                ToolbarButton(Icons.Default.MusicNote, "Audio EQ", onAudioEq)
                ToolbarButton(Icons.Default.Layers, "Adjust", onAdjustment)
                if (hasSelection) {
                    ToolbarButton(Icons.Default.Delete, "Ripple", onRippleDelete)
                } else {
                    ToolbarButton(Icons.Default.Layers, "Track", onSelectTrack)
                }
            }
        }
    }
}

@Composable
private fun ToolbarButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ToolbarSpacer() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.size(48.dp)
    ) {}
}
