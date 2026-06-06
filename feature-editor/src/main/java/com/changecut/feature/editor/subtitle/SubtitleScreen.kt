package com.changecut.feature.editor.subtitle

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleScreen(
    currentTimeUs: Long = 0L,
    existingSubtitles: List<SubtitleItem> = emptyList(),
    canAutoGenerate: Boolean = true,
    onAutoGenerate: () -> List<SubtitleItem> = { emptyList() },
    onBack: () -> Unit,
    onDone: (List<SubtitleItem>) -> Unit,
    viewModel: SubtitleViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<SubtitleItem?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    LaunchedEffect(currentTimeUs) {
        viewModel.setCurrentTime(currentTimeUs)
    }

    LaunchedEffect(existingSubtitles) {
        viewModel.setSubtitles(existingSubtitles)
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

    if (showAddDialog) {
        SubtitleEditDialog(
            initialText = "",
            initialStartUs = state.currentTimeUs,
            initialEndUs = state.currentTimeUs + 3_000_000L,
            onDismiss = { showAddDialog = false },
            onSave = { text, startUs, endUs ->
                viewModel.addSubtitle(startUs, endUs, text)
                showAddDialog = false
            }
        )
    }

    showEditDialog?.let { item ->
        SubtitleEditDialog(
            initialText = item.text,
            initialStartUs = item.startUs,
            initialEndUs = item.endUs,
            onDismiss = { showEditDialog = null },
            onSave = { text, startUs, endUs ->
                viewModel.updateSubtitle(item.id, text)
                viewModel.updateSubtitleTime(item.id, startUs, endUs)
                showEditDialog = null
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import SRT") },
            text = {
                Column {
                    Text("Paste SRT content:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        maxLines = 20
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.parseSRT(importText)
                    importText = ""
                    showImportDialog = false
                }) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subtitles") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onDone(state.subtitles) }) {
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, "Add", modifier = Modifier.padding(end = 4.dp))
                    Text("Add")
                }
                OutlinedButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FileUpload, "Import", modifier = Modifier.padding(end = 4.dp))
                    Text("Import SRT")
                }
                Button(
                    onClick = { viewModel.setSubtitles(onAutoGenerate()) },
                    modifier = Modifier.weight(1f),
                    enabled = canAutoGenerate && !state.isAutoGenerating
                ) {
                    Icon(Icons.Default.AutoAwesome, "Auto", modifier = Modifier.padding(end = 4.dp))
                    Text("Auto")
                }
            }
        }
    ) { padding ->
        if (state.subtitles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Subtitles,
                        contentDescription = null,
                        modifier = Modifier.padding(bottom = 8.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("No subtitles yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Add subtitles manually or import an SRT file",
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                itemsIndexed(state.subtitles, key = { _, item -> item.id }) { index, item ->
                    SubtitleCard(
                        index = index + 1,
                        item = item,
                        isActive = state.currentTimeUs in item.startUs..item.endUs,
                        onEdit = { showEditDialog = item },
                        onDelete = { viewModel.removeSubtitle(item.id) }
                    )
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun SubtitleCard(
    index: Int,
    item: SubtitleItem,
    isActive: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val bgColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surface
    val textColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "$index",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${formatSrtTimeUs(item.startUs)} \u2192 ${formatSrtTimeUs(item.endUs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
            Column {
                IconButton(onClick = onEdit, modifier = Modifier.padding(0.dp)) {
                    Icon(Icons.Default.Edit, "Edit", modifier = Modifier.padding(4.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.padding(0.dp)) {
                    Icon(
                        Icons.Default.Delete, "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SubtitleEditDialog(
    initialText: String,
    initialStartUs: Long,
    initialEndUs: Long,
    onDismiss: () -> Unit,
    onSave: (text: String, startUs: Long, endUs: Long) -> Unit
) {
    var text by remember(initialText) { mutableStateOf(initialText) }
    var startText by remember(initialStartUs) { mutableStateOf(formatSrtTimeUs(initialStartUs)) }
    var endText by remember(initialEndUs) { mutableStateOf(formatSrtTimeUs(initialEndUs)) }
    var timeError by remember(initialText, initialStartUs, initialEndUs) { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Subtitle Text") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5,
                    label = { Text("Enter subtitle text") }
                )
                OutlinedTextField(
                    value = startText,
                    onValueChange = {
                        startText = it
                        timeError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Start time") },
                    supportingText = { Text("Format: HH:MM:SS,mmm") }
                )
                OutlinedTextField(
                    value = endText,
                    onValueChange = {
                        endText = it
                        timeError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("End time") },
                    supportingText = { Text("Format: HH:MM:SS,mmm") }
                )
                if (!timeError.isNullOrBlank()) {
                    Text(
                        text = timeError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val startUs = parseSrtTimeInput(startText)
                val endUs = parseSrtTimeInput(endText)
                when {
                    startUs == null || endUs == null -> timeError = "Invalid time format"
                    endUs <= startUs -> timeError = "End time must be after start time"
                    else -> onSave(text, startUs, endUs)
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun parseSrtTimeInput(value: String): Long? {
    val match = Regex("(\\d{2}):(\\d{2}):(\\d{2})[,\\.](\\d{3})").matchEntire(value.trim()) ?: return null
    val hours = match.groupValues[1].toLongOrNull() ?: return null
    val minutes = match.groupValues[2].toLongOrNull() ?: return null
    val seconds = match.groupValues[3].toLongOrNull() ?: return null
    val millis = match.groupValues[4].toLongOrNull() ?: return null
    return ((hours * 3600 + minutes * 60 + seconds) * 1_000_000L) + (millis * 1000L)
}

private fun formatSrtTimeUs(us: Long): String {
    val totalMs = us / 1000
    val h = totalMs / 3600000
    val m = (totalMs % 3600000) / 60000
    val s = (totalMs % 60000) / 1000
    val ms = totalMs % 1000
    return String.format("%02d:%02d:%02d,%03d", h, m, s, ms)
}
