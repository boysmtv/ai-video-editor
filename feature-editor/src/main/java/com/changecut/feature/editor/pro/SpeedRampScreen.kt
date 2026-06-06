package com.changecut.feature.editor.pro

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.util.UUID

data class SpeedControlPoint(
    val id: String,
    val timeFraction: Float,
    val speed: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedRampScreen(
    totalDurationUs: Long,
    initialPoints: List<SpeedControlPoint> = emptyList(),
    onNavigateBack: () -> Unit,
    onApply: (List<SpeedControlPoint>) -> Unit
) {
    var controlPoints by remember(initialPoints, totalDurationUs) {
        mutableStateOf(
            if (initialPoints.isNotEmpty()) {
                initialPoints.sortedBy { it.timeFraction }
            } else {
                listOf(
                    SpeedControlPoint(UUID.randomUUID().toString(), 0f, 1f),
                    SpeedControlPoint(UUID.randomUUID().toString(), 1f, 1f)
                )
            }
        )
    }
    var selectedPointId by remember(initialPoints, totalDurationUs) { mutableStateOf<String?>(null) }
    var editSpeedValue by remember(initialPoints, totalDurationUs) { mutableStateOf(1f) }

    val selectedPoint = controlPoints.find { it.id == selectedPointId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Speed Ramp") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { onApply(controlPoints) }) {
                        Icon(Icons.Default.Check, "Apply")
                        Spacer(Modifier.width(4.dp))
                        Text("Apply")
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
                text = "Duration: ${formatSpeedRampTime(totalDurationUs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SpeedGraphCard(
                controlPoints = controlPoints,
                selectedPointId = selectedPointId,
                onSelectPoint = { selectedPointId = it; it?.let { id ->
                    editSpeedValue = controlPoints.find { p -> p.id == id }?.speed ?: 1f
                } },
                onAddPoint = { fraction, speed ->
                    val newPoint = SpeedControlPoint(
                        id = UUID.randomUUID().toString(),
                        timeFraction = fraction,
                        speed = speed
                    )
                    controlPoints = (controlPoints + newPoint)
                        .sortedBy { it.timeFraction }
                    selectedPointId = newPoint.id
                    editSpeedValue = speed
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val newFraction = controlPoints.lastOrNull { it.timeFraction < 1f }
                            ?.let { (it.timeFraction + 0.2f).coerceAtMost(1f) } ?: 0.5f
                        val newSpeed = 1f
                        val newPoint = SpeedControlPoint(
                            id = UUID.randomUUID().toString(),
                            timeFraction = newFraction,
                            speed = newSpeed
                        )
                        controlPoints = (controlPoints + newPoint)
                            .sortedBy { it.timeFraction }
                        selectedPointId = newPoint.id
                        editSpeedValue = newSpeed
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, "Add")
                    Spacer(Modifier.width(4.dp))
                    Text("Add Point")
                }
                OutlinedButton(
                    onClick = {
                        selectedPointId?.let { id ->
                            if (controlPoints.size > 2) {
                                controlPoints = controlPoints.filter { it.id != id }
                                selectedPointId = null
                            }
                        }
                    },
                    enabled = selectedPointId != null && controlPoints.size > 2,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, "Remove")
                    Spacer(Modifier.width(4.dp))
                    Text("Remove")
                }
            }

            selectedPoint?.let { point ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Speed: ${"%.1f".format(point.speed)}x",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "Time: ${formatSpeedRampTime((totalDurationUs * point.timeFraction).toLong())}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Slider(
                            value = point.speed,
                            onValueChange = { newSpeed ->
                                editSpeedValue = newSpeed
                                controlPoints = controlPoints.map { cp ->
                                    if (cp.id == point.id) cp.copy(speed = newSpeed) else cp
                                }
                            },
                            valueRange = 0.1f..10f,
                            steps = 98,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("0.1x", style = MaterialTheme.typography.labelSmall)
                            Text("10x", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Presets", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SpeedPresetChip("Slow Mo", 0.5f) { presetSpeed ->
                            controlPoints = controlPoints.map { cp ->
                                if (cp.id == selectedPointId) cp.copy(speed = presetSpeed) else cp
                            }
                            editSpeedValue = presetSpeed
                        }
                        SpeedPresetChip("Normal", 1f) { presetSpeed ->
                            controlPoints = controlPoints.map { cp ->
                                if (cp.id == selectedPointId) cp.copy(speed = presetSpeed) else cp
                            }
                            editSpeedValue = presetSpeed
                        }
                        SpeedPresetChip("Fast", 2f) { presetSpeed ->
                            controlPoints = controlPoints.map { cp ->
                                if (cp.id == selectedPointId) cp.copy(speed = presetSpeed) else cp
                            }
                            editSpeedValue = presetSpeed
                        }
                        SpeedPresetChip("Timelapse", 4f) { presetSpeed ->
                            controlPoints = controlPoints.map { cp ->
                                if (cp.id == selectedPointId) cp.copy(speed = presetSpeed) else cp
                            }
                            editSpeedValue = presetSpeed
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedPresetChip(
    label: String,
    speed: Float,
    onClick: (Float) -> Unit
) {
    FilterChip(
        selected = false,
        onClick = { onClick(speed) },
        label = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, style = MaterialTheme.typography.labelSmall)
                Text(
                    "${speed}x",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

private fun formatSpeedRampTime(timeUs: Long): String {
    val totalMs = (timeUs / 1000L).coerceAtLeast(0L)
    val minutes = totalMs / 60_000L
    val seconds = (totalMs % 60_000L) / 1_000L
    val millis = totalMs % 1_000L
    return String.format("%02d:%02d.%03d", minutes, seconds, millis)
}

@Composable
private fun SpeedGraphCard(
    controlPoints: List<SpeedControlPoint>,
    selectedPointId: String?,
    onSelectPoint: (String?) -> Unit,
    onAddPoint: (Float, Float) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .clickable(enabled = false) { }
            ) {
                val w = size.width
                val h = size.height
                val padding = 20f
                val graphW = w - padding * 2
                val graphH = h - padding * 2
                val maxSpeed = controlPoints.maxOfOrNull { it.speed }?.coerceAtLeast(1f) ?: 1f

                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = Offset(padding, padding),
                    end = Offset(padding, h - padding),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = Offset(padding, h - padding),
                    end = Offset(w - padding, h - padding),
                    strokeWidth = 1f
                )

                drawLine(
                    color = Color.Gray.copy(alpha = 0.15f),
                    start = Offset(padding, h - padding - graphH / 2f),
                    end = Offset(w - padding, h - padding - graphH / 2f),
                    strokeWidth = 1f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(4f, 4f), 0f
                    )
                )

                if (controlPoints.size >= 2) {
                    val path = Path()
                    val sorted = controlPoints.sortedBy { it.timeFraction }
                    sorted.forEachIndexed { i, point ->
                        val x = padding + point.timeFraction * graphW
                        val y = h - padding - (point.speed / maxSpeed) * graphH
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF00BCD4),
                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                    )
                }

                controlPoints.forEach { point ->
                    val x = padding + point.timeFraction * graphW
                    val y = h - padding - (point.speed / maxSpeed) * graphH
                    val isSelected = point.id == selectedPointId
                    val dotColor = if (isSelected) Color(0xFFFF5722) else Color(0xFF00BCD4)
                    drawCircle(
                        color = dotColor,
                        radius = if (isSelected) 10f else 7f,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = if (isSelected) 5f else 4f,
                        center = Offset(x, y)
                    )
                }
            }

            if (controlPoints.size < 2) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Add at least 2 speed control points",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
