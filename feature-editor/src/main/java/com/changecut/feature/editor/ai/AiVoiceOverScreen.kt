package com.changecut.feature.editor.ai

import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiVoiceOverScreen(
    onNavigateBack: () -> Unit,
    onDone: () -> Unit,
    onImportToTimeline: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val ttsEngine = remember { com.changecut.core.ai.AiTextToSpeech(context) }
    var script by remember { mutableStateOf("") }
    var selectedVoice by remember { mutableStateOf(com.changecut.core.ai.VoiceType.NEUTRAL) }
    var speed by remember { mutableFloatStateOf(1.0f) }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedAudioPath by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var generationProgress by remember { mutableFloatStateOf(0f) }

    val availableVoices = remember { ttsEngine.getAvailableVoices() }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            ttsEngine.cleanup()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Voice Over") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Default.Check, "Done")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Voice Over Script",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = script,
                onValueChange = { script = it },
                label = { Text("Enter your script") },
                placeholder = { Text("Type the text you want to convert to speech...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                minLines = 4,
                maxLines = 8
            )

            Text(
                text = "Voice Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                availableVoices.forEach { voice ->
                    FilterChip(
                        selected = selectedVoice == voice,
                        onClick = { selectedVoice = voice },
                        label = { Text(voice.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text(
                text = "Speed: ${String.format("%.1f", speed)}x",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Slider(
                value = speed,
                onValueChange = { speed = it.coerceIn(0.5f, 2.0f) },
                valueRange = 0.5f..2.0f,
                steps = 14,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0.5x", style = MaterialTheme.typography.bodySmall)
                Text("1.0x", style = MaterialTheme.typography.bodySmall)
                Text("2.0x", style = MaterialTheme.typography.bodySmall)
            }

            if (isGenerating) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Generating voice over...")
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { generationProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Button(
                onClick = {
                    if (script.isBlank()) {
                        Toast.makeText(context, "Please enter a script", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isGenerating = true
                    generationProgress = 0.3f
                    CoroutineScope(Dispatchers.IO).launch {
                        val outputFile = File(context.cacheDir, "voiceover/voiceover_${System.currentTimeMillis()}.wav")
                        outputFile.parentFile?.mkdirs()
                        generationProgress = 0.6f
                        val result = ttsEngine.generateVoiceOver(
                            text = script,
                            voiceType = selectedVoice,
                            speed = speed,
                            outputPath = outputFile.absolutePath
                        )
                        withContext(Dispatchers.Main) {
                            generationProgress = 1f
                            isGenerating = false
                            result.onSuccess { path ->
                                generatedAudioPath = path
                                Toast.makeText(context, "Voice over generated", Toast.LENGTH_SHORT).show()
                            }.onFailure { e ->
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                enabled = script.isNotBlank() && !isGenerating,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Generate Voice Over")
            }

            if (generatedAudioPath != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Generated Voice Over",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Voice: ${selectedVoice.displayName}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Speed: ${String.format("%.1f", speed)}x",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val path = generatedAudioPath ?: return@OutlinedButton
                                    if (isPlaying) {
                                        mediaPlayer?.stop()
                                        mediaPlayer?.release()
                                        mediaPlayer = null
                                        isPlaying = false
                                    } else {
                                        try {
                                            mediaPlayer = MediaPlayer().apply {
                                                setDataSource(path)
                                                setOnCompletionListener {
                                                    isPlaying = false
                                                }
                                                prepare()
                                                start()
                                                isPlaying = true
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Playback failed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (isPlaying) "Stop" else "Preview")
                            }

                            Button(
                                onClick = {
                                    generatedAudioPath?.let { path ->
                                        onImportToTimeline(path)
                                        Toast.makeText(context, "Added to timeline", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Timeline, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Import to Timeline")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
