package com.changecut.feature.editor.effects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.changecut.core.ffmpeg.TransitionEngine
import com.changecut.core.editor.TransitionDefFull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransitionPickerScreen(
    initialTransitionId: String? = null,
    initialDurationMs: Int = 500,
    onNavigateBack: () -> Unit,
    onDone: (transitionId: String?, durationMs: Int) -> Unit
) {
    val transitions = remember { TransitionEngine.transitions }
    val fallbackTransitionId = transitions.firstOrNull()?.id.orEmpty()
    var selectedTransitionId by remember {
        mutableStateOf(initialTransitionId?.takeIf { id -> transitions.any { it.id == id } } ?: fallbackTransitionId)
    }
    var durationMs by remember { mutableFloatStateOf(initialDurationMs.coerceIn(100, 2000).toFloat()) }

    LaunchedEffect(initialTransitionId, initialDurationMs, transitions) {
        selectedTransitionId = initialTransitionId
            ?.takeIf { id -> transitions.any { it.id == id } }
            ?: fallbackTransitionId
        durationMs = initialDurationMs.coerceIn(100, 2000).toFloat()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transitions") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    Text(
                        text = "Clear",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { onDone(null, 0) }
                    )
                    IconButton(onClick = { onDone(selectedTransitionId, durationMs.toInt()) }) {
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
        ) {
            TransitionGrid(
                transitions = transitions,
                selectedId = selectedTransitionId,
                onTransitionSelected = { selectedTransitionId = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Duration: ${durationMs.toInt()}ms",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = durationMs,
                        onValueChange = { durationMs = it },
                        valueRange = 100f..2000f,
                        steps = 18,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onDone(selectedTransitionId, durationMs.toInt()) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Apply", modifier = Modifier.padding(end = 8.dp))
                        Text("Apply Transition")
                    }
                }
            }
        }
    }
}

@Composable
private fun TransitionGrid(
    transitions: List<TransitionDefFull>,
    selectedId: String,
    onTransitionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(transitions, key = { it.id }) { transition ->
            TransitionCard(
                transition = transition,
                isSelected = transition.id == selectedId,
                onClick = { onTransitionSelected(transition.id) }
            )
        }
    }
}

@Composable
private fun TransitionCard(
    transition: TransitionDefFull,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                2.dp, MaterialTheme.colorScheme.primary
            )
        } else null
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = transitionIcon(transition.id),
                    contentDescription = transition.name,
                    modifier = Modifier.padding(16.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = transition.name,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }
    }
}

private fun transitionIcon(id: String): ImageVector = when (id) {
    "fade" -> Icons.Default.FilterCenterFocus
    "slide_left", "slide_right" -> Icons.Default.SwapHoriz
    "slide_up", "slide_down" -> Icons.Default.UnfoldMore
    "wipe_left", "wipe_right" -> Icons.Default.ArrowForward
    "dissolve" -> Icons.Default.PlayArrow
    "zoom_in" -> Icons.Default.ZoomIn
    "radial" -> Icons.Default.ZoomOut
    else -> Icons.Default.FastForward
}
