package com.changecut.feature.editor.timeline

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TimelineZoomControl(
    zoomLevel: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingZoom by remember { mutableFloatStateOf(zoomLevel) }

    LaunchedEffect(zoomLevel) {
        pendingZoom = zoomLevel
    }

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onZoomChange(zoomLevel - 0.5f) }) {
            Icon(Icons.Default.Remove, contentDescription = "Zoom out")
        }
        Slider(
            value = pendingZoom,
            onValueChange = { pendingZoom = it },
            onValueChangeFinished = {
                if (pendingZoom != zoomLevel) {
                    onZoomChange(pendingZoom)
                }
            },
            valueRange = 0.1f..10f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
        IconButton(onClick = { onZoomChange(zoomLevel + 0.5f) }) {
            Icon(Icons.Default.Add, contentDescription = "Zoom in")
        }
        Text(
            text = "${(pendingZoom * 100).toInt()}%",
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
