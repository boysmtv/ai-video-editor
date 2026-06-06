package com.changecut.feature.editor.keyframe

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.changecut.core.editor.EasingType
import com.changecut.core.editor.KeyframeProperty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyframeEditorScreen(
    clipId: String,
    existingKeyframes: List<KeyframeItem>,
    onNavigateBack: () -> Unit,
    onDone: (List<KeyframeItem>) -> Unit,
    viewModel: KeyframeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(clipId, existingKeyframes) {
        viewModel.initialize(clipId, existingKeyframes)
    }

    var propertyExpanded by remember { mutableStateOf(false) }
    var sliderValue by remember(state.selectedKeyframeId) {
        mutableStateOf(
            state.keyframes.find { it.id == state.selectedKeyframeId }?.value ?: 0f
        )
    }

    LaunchedEffect(state.selectedKeyframeId, state.selectedProperty, state.keyframes) {
        sliderValue = state.keyframes.find { it.id == state.selectedKeyframeId }?.value ?: sliderValue
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Keyframe Editor") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { onDone(state.allKeyframes) }) {
                        Icon(Icons.Default.Check, "Done")
                        Spacer(Modifier.width(4.dp))
                        Text("Done")
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
            PreviewArea(
                interpolatedValue = state.interpolatedValue,
                selectedProperty = state.selectedProperty
            )

            PropertySelector(
                selectedProperty = state.selectedProperty,
                expanded = propertyExpanded,
                onExpandedChange = { propertyExpanded = it },
                onPropertySelected = { viewModel.selectProperty(it) }
            )

            TimelineBar(
                keyframes = state.keyframes,
                currentTimeUs = state.currentTimeUs,
                totalDurationUs = state.allKeyframes.maxOfOrNull { it.timeUs } ?: 1_000_000L,
                onTimeSelected = { viewModel.setCurrentTime(it) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.addKeyframe(state.currentTimeUs, sliderValue) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, "Add")
                    Spacer(Modifier.width(4.dp))
                    Text("Add Keyframe")
                }
                OutlinedButton(
                    onClick = {
                        state.selectedKeyframeId?.let { viewModel.removeKeyframe(it) }
                    },
                    enabled = state.selectedKeyframeId != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, "Remove")
                    Spacer(Modifier.width(4.dp))
                    Text("Remove")
                }
            }

            state.selectedKeyframeId?.let { selectedId ->
                val selectedKf = state.keyframes.find { it.id == selectedId }
                if (selectedKf != null) {
                    val valueRange = propertyValueRange(state.selectedProperty)
                    EditKeyframePanel(
                        property = state.selectedProperty,
                        value = selectedKf.value,
                        valueRange = valueRange,
                        easing = selectedKf.easing,
                        onValueChange = { newVal ->
                            sliderValue = newVal
                            viewModel.updateKeyframeValue(selectedId, newVal)
                        },
                        onEasingChange = { viewModel.updateKeyframeEasing(selectedId, it) }
                    )
                }
            }

            HorizontalDivider()

            KeyframeList(
                keyframes = state.keyframes,
                selectedId = state.selectedKeyframeId,
                onSelect = { viewModel.selectKeyframe(it) }
            )
        }
    }
}

@Composable
private fun PreviewArea(
    interpolatedValue: Float,
    selectedProperty: KeyframeProperty
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${selectedProperty.name}: ${"%.2f".format(interpolatedValue)}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(
                                width = (120 * interpolatedValue.coerceIn(0.1f, 1f)).dp,
                                height = (120 * interpolatedValue.coerceIn(0.1f, 1f)).dp
                            )
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PropertySelector(
    selectedProperty: KeyframeProperty,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onPropertySelected: (KeyframeProperty) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
        Text(
            text = "Property: ${selectedProperty.name}",
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .clickable { onExpandedChange(true) }
                .padding(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            KeyframeProperty.entries.forEach { prop ->
                DropdownMenuItem(
                    text = { Text(prop.name) },
                    onClick = {
                        onPropertySelected(prop)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@Composable
private fun TimelineBar(
    keyframes: List<KeyframeItem>,
    currentTimeUs: Long,
    totalDurationUs: Long,
    onTimeSelected: (Long) -> Unit
) {
    Column {
        Text("Timeline", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = totalDurationUs > 0) {
                    // click handled by Canvas
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                if (totalDurationUs > 0) {
                    val playheadX = (currentTimeUs.toFloat() / totalDurationUs) * width
                    drawLine(
                        color = Color.Red,
                        start = Offset(playheadX, 0f),
                        end = Offset(playheadX, size.height),
                        strokeWidth = 2f
                    )
                    keyframes.forEach { kf ->
                        val x = (kf.timeUs.toFloat() / totalDurationUs) * width
                        drawCircle(
                            color = Color(0xFF00BCD4),
                            radius = 8f,
                            center = Offset(x, size.height / 2f)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 5f,
                            center = Offset(x, size.height / 2f)
                        )
                    }
                }
            }
        }
        Slider(
            value = currentTimeUs.toFloat().coerceIn(0f, totalDurationUs.toFloat().coerceAtLeast(1f)),
            onValueChange = { onTimeSelected(it.toLong()) },
            valueRange = 0f..totalDurationUs.toFloat().coerceAtLeast(1f),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EditKeyframePanel(
    property: KeyframeProperty,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    easing: EasingType,
    onValueChange: (Float) -> Unit,
    onEasingChange: (EasingType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Edit Keyframe", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Value: ${"%.2f".format(value)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text("Easing", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EasingType.entries.forEach { type ->
                    val isSelected = type == easing
                    TextButton(
                        onClick = { onEasingChange(type) },
                        modifier = Modifier
                            .then(
                                if (isSelected) Modifier.border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(4.dp)
                                ) else Modifier
                            )
                    ) {
                        Text(
                            text = type.name.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

private fun propertyValueRange(property: KeyframeProperty): ClosedFloatingPointRange<Float> {
    return when (property) {
        KeyframeProperty.POSITION_X,
        KeyframeProperty.POSITION_Y,
        KeyframeProperty.OPACITY -> 0f..1f
        KeyframeProperty.SCALE_X,
        KeyframeProperty.SCALE_Y -> 0.1f..2f
        KeyframeProperty.ROTATION -> -180f..180f
        KeyframeProperty.VOLUME -> 0f..2f
    }
}

@Composable
private fun KeyframeList(
    keyframes: List<KeyframeItem>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    Column {
        Text("Keyframes", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        if (keyframes.isEmpty()) {
            Text(
                "No keyframes. Move playhead and tap Add.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            keyframes.sortedBy { it.timeUs }.forEach { kf ->
                val isSelected = kf.id == selectedId
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clickable { onSelect(kf.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Diamond,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF00BCD4)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "t=${kf.timeUs / 1000}ms",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "v=${"%.2f".format(kf.value)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Text(
                            kf.easing.name.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
