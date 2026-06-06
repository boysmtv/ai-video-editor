package com.changecut.feature.editor.ai

import android.widget.Toast
import androidx.compose.foundation.background
import com.changecut.core.ai.ScoredClip
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiAutoEditScreen(
    clipPaths: List<String>,
    onNavigateBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: AiAutoEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showCustomDuration by remember { mutableStateOf(false) }
    var customDurationText by remember { mutableStateOf("") }
    var selectedClips by remember(clipPaths) { mutableStateOf(clipPaths) }

    LaunchedEffect(clipPaths) {
        selectedClips = selectedClips.filter { it in clipPaths }
        if (selectedClips.isEmpty()) {
            selectedClips = clipPaths
        }
    }

    LaunchedEffect(showCustomDuration, state.targetDurationMs) {
        if (showCustomDuration) {
            customDurationText = state.targetDurationMs.toString()
        }
    }

    val durationOptions = listOf(
        15_000L to "15s",
        30_000L to "30s",
        60_000L to "60s",
        0L to "Custom"
    )

    val styleOptions = EditStyle.entries

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Auto Edit") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (state.analysisStep == AnalysisStep.COMPLETE && state.autoEditResult != null) {
                        IconButton(onClick = onDone) {
                            Icon(Icons.Default.Check, "Done")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.analysisStep == AnalysisStep.IDLE) {
                ClipsSelectionSection(
                    clipPaths = clipPaths,
                    selectedClips = selectedClips,
                    onToggleClip = { path ->
                        selectedClips = if (path in selectedClips) {
                            selectedClips - path
                        } else {
                            selectedClips + path
                        }
                    }
                )

                Text(
                    text = "Target Duration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    durationOptions.forEach { (durationMs, label) ->
                        FilterChip(
                            selected = if (durationMs == 0L) showCustomDuration
                            else state.targetDurationMs == durationMs,
                            onClick = {
                                if (durationMs == 0L) {
                                    showCustomDuration = true
                                } else {
                                    showCustomDuration = false
                                    viewModel.setTargetDuration(durationMs)
                                }
                            },
                            label = { Text(label) },
                            leadingIcon = if (durationMs == 0L) null
                            else {
                                { Icon(Icons.Default.Timer, null, modifier = Modifier.size(16.dp)) }
                            }
                        )
                    }
                }

                if (showCustomDuration) {
                    OutlinedTextField(
                        value = customDurationText,
                        onValueChange = { value ->
                            customDurationText = value
                            value.toLongOrNull()?.let { ms ->
                                viewModel.setTargetDuration(ms.coerceIn(5_000L, 600_000L))
                            }
                        },
                        label = { Text("Duration (ms)") },
                        placeholder = { Text("30000") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text(
                    text = "Edit Style",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    styleOptions.forEach { style ->
                        FilterChip(
                            selected = state.selectedStyle == style,
                            onClick = { viewModel.setStyle(style) },
                            label = { Text(formatStyleName(style)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Button(
                    onClick = {
                        viewModel.setClips(selectedClips)
                        viewModel.analyzeClips()
                    },
                    enabled = selectedClips.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Analytics, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Analyze Clips")
                }
            }

            if (state.analysisStep == AnalysisStep.ANALYZING ||
                state.analysisStep == AnalysisStep.SCORING ||
                state.analysisStep == AnalysisStep.BUILDING
            ) {
                AnalysisProgressSection(
                    step = state.analysisStep,
                    progress = state.analysisProgress
                )
            }

            if (state.analyzedClips.isNotEmpty()) {
                AnalyzedClipsSection(
                    scoredClips = state.analyzedClips,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (state.analysisStep == AnalysisStep.COMPLETE &&
                state.analyzedClips.isNotEmpty() &&
                state.autoEditResult == null
            ) {
                Button(
                    onClick = { viewModel.generateAutoEdit() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Auto Edit - Generate Draft Timeline")
                }
            }

            if (state.autoEditResult != null) {
                AutoEditResultSection(
                    result = state.autoEditResult!!,
                    onApply = { viewModel.applyAutoEdit() },
                    onRegenerate = { viewModel.generateAutoEdit() },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (state.autoEditResult != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.generateIntro() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Generate Intro")
                    }
                    OutlinedButton(
                        onClick = { viewModel.generateHighlightReel() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Highlight Reel")
                    }
                }
            }
        }
    }

    if (state.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(state.error ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun ClipsSelectionSection(
    clipPaths: List<String>,
    selectedClips: List<String>,
    onToggleClip: (String) -> Unit
) {
    Column {
        Text(
            text = "Select clips to analyze (${selectedClips.size}/${clipPaths.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false
        ) {
            items(clipPaths) { path ->
                val isSelected = path in selectedClips
                Box(
                    modifier = Modifier
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .then(
                            if (isSelected) Modifier.border(
                                2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)
                            ) else Modifier
                        )
                        .clickable { onToggleClip(path) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Analytics,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = path.substringAfterLast("/").substringBefore("."),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalysisProgressSection(step: AnalysisStep, progress: Float) {
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
            Text(
                text = when (step) {
                    AnalysisStep.ANALYZING -> "Analyzing clips..."
                    AnalysisStep.SCORING -> "Scoring best moments..."
                    AnalysisStep.BUILDING -> "Building draft timeline..."
                    else -> "Processing..."
                },
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AnalyzedClipsSection(
    scoredClips: List<ScoredClip>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Analysis Results",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        scoredClips.take(10).forEach { scored ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Score: ${(scored.score.total * 100).toInt()}%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${scored.startTimeUs / 1_000_000}s - ${scored.endTimeUs / 1_000_000}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "S:${(scored.score.stability * 100).toInt()}% B:${(scored.score.brightness * 100).toInt()}% M:${(scored.score.motion * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (scored.score.total > 0.7) Color(0xFF4CAF50)
                                else if (scored.score.total > 0.4) Color(0xFFFF9800)
                                else Color(0xFFF44336)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${(scored.score.total * 100).toInt()}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AutoEditResultSection(
    result: AutoEditResult,
    onApply: () -> Unit,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Draft Timeline Ready",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text("${result.clips.size} clips | ${result.totalDurationMs / 1000}s total")
            Text("Style: ${formatStyleName(result.style)}")
            Text("${result.transitions.size} transitions")
            Spacer(Modifier.height(8.dp))

            result.clips.forEach { clip ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = clip.label,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${clip.durationUs / 1_000_000}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onApply,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Apply to Timeline")
                }
                OutlinedButton(
                    onClick = onRegenerate,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Regenerate")
                }
            }
        }
    }
}

private fun formatStyleName(style: EditStyle): String {
    return style.name.replace("_", " ").lowercase()
        .replaceFirstChar { it.uppercase() }
}
