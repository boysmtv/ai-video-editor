package com.changecut.feature.editor.audio

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTrackScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: AudioTrackViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri -> viewModel.importAudio(uri) }
    }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.recordVoiceOver()
        } else {
            viewModel.clearError()
        }
    }

    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) { Text("OK") }
            }
        )
    }

    showDeleteConfirm?.let { trackId ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Remove Track") },
            text = { Text("Remove this audio track?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeTrack(trackId)
                    showDeleteConfirm = null
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Tracks") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Default.Check, "Done")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { audioPickerLauncher.launch("audio/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FileUpload, "Import", modifier = Modifier.padding(end = 4.dp))
                    Text("Import")
                }
                Button(
                    onClick = {
                        if (state.isRecording) viewModel.stopRecording()
                        else {
                            val permission = Manifest.permission.RECORD_AUDIO
                            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                viewModel.recordVoiceOver()
                            } else {
                                recordPermissionLauncher.launch(permission)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (state.isRecording) Icons.Default.Check else Icons.Default.Mic,
                        "Record",
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(if (state.isRecording) "Stop" else "Record")
                }
            }
        }
    ) { padding ->
        if (state.audioTracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.padding(bottom = 8.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "No audio tracks yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Import audio or record a voice over",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.audioTracks, key = { it.id }) { track ->
                    AudioTrackCard(
                        track = track,
                        isRecording = state.isRecording,
                        onVolumeChange = { viewModel.setVolume(track.id, it) },
                        onFadeInChange = { viewModel.setFadeIn(track.id, it) },
                        onFadeOutChange = { viewModel.setFadeOut(track.id, it) },
                        onDelete = { showDeleteConfirm = track.id }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun AudioTrackCard(
    track: AudioTrackInfo,
    isRecording: Boolean,
    onVolumeChange: (Float) -> Unit,
    onFadeInChange: (Int) -> Unit,
    onFadeOutChange: (Int) -> Unit,
    onDelete: () -> Unit
) {
    var pendingVolume by remember(track.id) { mutableStateOf(track.volume) }
    var pendingFadeIn by remember(track.id) { mutableStateOf(track.fadeInMs.toFloat()) }
    var pendingFadeOut by remember(track.id) { mutableStateOf(track.fadeOutMs.toFloat()) }

    LaunchedEffect(track.id, track.volume, track.fadeInMs, track.fadeOutMs) {
        pendingVolume = track.volume
        pendingFadeIn = track.fadeInMs.toFloat()
        pendingFadeOut = track.fadeOutMs.toFloat()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column {
                        Text(track.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            formatDuration(track.durationMs),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Start ${formatDuration(track.startOffsetUs / 1000L)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error)
                }
            }

            WaveformPlaceholder(modifier = Modifier.padding(vertical = 8.dp))

            Text("Volume: ${(pendingVolume * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
            Slider(
                value = pendingVolume,
                onValueChange = { pendingVolume = it },
                onValueChangeFinished = {
                    if (pendingVolume != track.volume) {
                        onVolumeChange(pendingVolume)
                    }
                },
                valueRange = 0f..2f,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Fade In: ${pendingFadeIn.toInt()}ms", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = pendingFadeIn,
                        onValueChange = { pendingFadeIn = it },
                        onValueChangeFinished = {
                            val committed = pendingFadeIn.toInt()
                            if (committed != track.fadeInMs) {
                                onFadeInChange(committed)
                            }
                        },
                        valueRange = 0f..5000f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Fade Out: ${pendingFadeOut.toInt()}ms", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = pendingFadeOut,
                        onValueChange = { pendingFadeOut = it },
                        onValueChangeFinished = {
                            val committed = pendingFadeOut.toInt()
                            if (committed != track.fadeOutMs) {
                                onFadeOutChange(committed)
                            }
                        },
                        valueRange = 0f..5000f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun WaveformPlaceholder(modifier: Modifier = Modifier) {
    val barCount = 40
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val barWidth = size.width / barCount
        for (i in 0 until barCount) {
            val heightMultiplier = when {
                i < barCount / 4 -> (i.toFloat() / (barCount / 4))
                i > (barCount * 3) / 4 -> ((barCount - i).toFloat() / (barCount / 4))
                else -> 0.5f + (kotlin.math.sin(i * 0.5) * 0.5).toFloat().coerceIn(0.2f, 1f)
            }
            val barHeight = (size.height * heightMultiplier * 0.7f).coerceAtLeast(2f)
            drawRect(
                color = Color(0xFF6C63FF).copy(alpha = 0.6f),
                topLeft = Offset(i * barWidth + 1f, (size.height - barHeight) / 2f),
                size = androidx.compose.ui.geometry.Size(
                    (barWidth - 2f).coerceAtLeast(1f),
                    barHeight
                )
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSec = ms / 1000
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return String.format("%02d:%02d", m, s)
}
