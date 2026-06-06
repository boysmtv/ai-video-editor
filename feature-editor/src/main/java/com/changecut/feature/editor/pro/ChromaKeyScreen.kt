package com.changecut.feature.editor.pro

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.changecut.core.ffmpeg.ChromaKeyEngine

data class ChromaKeyColor(
    val label: String,
    val hex: String,
    val color: Color
)

private val PRESET_COLORS = listOf(
    ChromaKeyColor("Green", "0x00FF00", Color(0xFF00FF00)),
    ChromaKeyColor("Blue", "0x0000FF", Color(0xFF0000FF)),
    ChromaKeyColor("Red", "0xFF0000", Color(0xFFFF0000)),
    ChromaKeyColor("Magenta", "0xFF00FF", Color(0xFFFF00FF)),
    ChromaKeyColor("White", "0xFFFFFF", Color(0xFFFFFFFF)),
    ChromaKeyColor("Custom", "", Color.Gray)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChromaKeyScreen(
    videoPath: String,
    initialColorFilter: String? = null,
    onNavigateBack: () -> Unit,
    onClear: () -> Unit,
    onApply: (colorHex: String, similarity: Float, smoothness: Float) -> Unit,
    chromaKeyEngine: ChromaKeyEngine? = null
) {
    val initialSettings = remember(videoPath, initialColorFilter) { parseInitialChromaKey(initialColorFilter) }
    var selectedColorHex by remember(videoPath, initialColorFilter) { mutableStateOf(initialSettings?.first ?: "0x00FF00") }
    var similarity by remember(videoPath, initialColorFilter) { mutableStateOf(initialSettings?.second ?: 0.2f) }
    var blend by remember(videoPath, initialColorFilter) { mutableStateOf(initialSettings?.third ?: 0.1f) }
    var replaceBg by remember(videoPath) { mutableStateOf(false) }
    var bgFilePath by remember(videoPath) { mutableStateOf<String?>(null) }
    var isProcessing by remember(videoPath) { mutableStateOf(false) }
    var showCustomColorDialog by remember(videoPath) { mutableStateOf(false) }
    var customColorHex by remember(videoPath) { mutableStateOf("") }
    var previewPath by remember(videoPath) { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val bgPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { bgFilePath = it.toString() }
    }

    if (showCustomColorDialog) {
        AlertDialog(
            onDismissRequest = { showCustomColorDialog = false },
            title = { Text("Custom Color Hex") },
            text = {
                Column {
                    Text("Enter hex color (e.g. 0xRRGGBB):")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = customColorHex.ifBlank { "0x" },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (customColorHex.length >= 8) {
                        selectedColorHex = customColorHex
                        showCustomColorDialog = false
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomColorDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chroma Key") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = onClear,
                        enabled = !isProcessing
                    ) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(
                        onClick = {
                            isProcessing = true
                            previewPath = null
                            onApply(selectedColorHex, similarity, blend)
                            isProcessing = false
                        },
                        enabled = !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        } else {
                            Icon(Icons.Default.Check, "Apply")
                            Spacer(Modifier.width(4.dp))
                            Text("Apply")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (previewPath != null) {
                        Text("Preview Available", style = MaterialTheme.typography.bodyLarge)
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Chroma Key Preview",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Select color and adjust sliders",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Column {
                Text("Key Color", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PRESET_COLORS.forEach { preset ->
                        val isSelected = preset.hex == selectedColorHex
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    if (preset.label == "Custom") {
                                        showCustomColorDialog = true
                                    } else {
                                        selectedColorHex = preset.hex
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(preset.color)
                                    .then(
                                        if (isSelected) Modifier.border(
                                            3.dp,
                                            MaterialTheme.colorScheme.primary,
                                            CircleShape
                                        ) else Modifier.border(
                                            1.dp,
                                            Color.Gray.copy(alpha = 0.3f),
                                            CircleShape
                                        )
                                    )
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                preset.label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            Column {
                Text(
                    "Similarity: ${"%.2f".format(similarity)}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    "Higher = more aggressive keying",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Slider(
                    value = similarity,
                    onValueChange = { similarity = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column {
                Text(
                    "Blend: ${"%.2f".format(blend)}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    "Controls edge softness",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Slider(
                    value = blend,
                    onValueChange = { blend = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Replace Background", style = MaterialTheme.typography.titleSmall)
                        Switch(
                            checked = replaceBg,
                            onCheckedChange = { replaceBg = it }
                        )
                    }
                    if (replaceBg) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Background replacement UI is staged. Current render/export path only applies chroma key removal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { bgPickerLauncher.launch("video/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Image, "Select")
                            Spacer(Modifier.width(8.dp))
                            Text(
                                bgFilePath?.let { "Background selected" }
                                    ?: "Select background video/image"
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun parseInitialChromaKey(colorFilter: String?): Triple<String, Float, Float>? {
    val raw = colorFilter
        ?.takeIf { it.startsWith("chromakey:", ignoreCase = true) }
        ?.substringAfter(':')
        ?.trim()
        ?: return null
    val parts = raw.split(":")
    val color = parts.firstOrNull()
        ?.removePrefix("#")
        ?.removePrefix("0x")
        ?.removePrefix("0X")
        ?.takeIf { it.length == 6 }
        ?.uppercase()
        ?.let { "0x$it" }
        ?: return null
    val similarity = parts.getOrNull(1)?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.2f
    val blend = parts.getOrNull(2)?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.1f
    return Triple(color, similarity, blend)
}
