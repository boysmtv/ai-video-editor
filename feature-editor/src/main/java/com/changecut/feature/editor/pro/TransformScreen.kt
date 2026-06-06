package com.changecut.feature.editor.pro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.changecut.core.editor.EditorClip

@Composable
fun TransformScreen(
    clip: EditorClip?,
    onApply: (
        positionX: Float,
        positionY: Float,
        scaleX: Float,
        scaleY: Float,
        rotation: Float,
        opacity: Float,
        freezeDurationMs: Int
    ) -> Unit,
    onClose: () -> Unit
) {
    var positionX by remember(clip?.id) { mutableFloatStateOf(clip?.positionX ?: 0.5f) }
    var positionY by remember(clip?.id) { mutableFloatStateOf(clip?.positionY ?: 0.5f) }
    var scaleX by remember(clip?.id) { mutableFloatStateOf(clip?.scaleX ?: 1f) }
    var scaleY by remember(clip?.id) { mutableFloatStateOf(clip?.scaleY ?: 1f) }
    var rotation by remember(clip?.id) { mutableFloatStateOf(clip?.rotation ?: 0f) }
    var opacity by remember(clip?.id) { mutableFloatStateOf(clip?.opacity ?: 1f) }
    var freezeDurationMs by remember(clip?.id) { mutableIntStateOf(clip?.freezeDurationMs ?: 0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (clip == null) {
            Text(
                text = "Select a clip to edit transform values.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TransformSliderCard(
            title = "Position X",
            valueLabel = "${(positionX * 100).toInt()}%",
            value = positionX,
            range = 0f..1f,
            onValueChange = { positionX = it }
        )
        TransformSliderCard(
            title = "Position Y",
            valueLabel = "${(positionY * 100).toInt()}%",
            value = positionY,
            range = 0f..1f,
            onValueChange = { positionY = it }
        )
        TransformSliderCard(
            title = "Scale X",
            valueLabel = "${"%.2f".format(scaleX)}x",
            value = scaleX,
            range = 0.1f..2f,
            onValueChange = { scaleX = it }
        )
        TransformSliderCard(
            title = "Scale Y",
            valueLabel = "${"%.2f".format(scaleY)}x",
            value = scaleY,
            range = 0.1f..2f,
            onValueChange = { scaleY = it }
        )
        TransformSliderCard(
            title = "Rotation",
            valueLabel = "${rotation.toInt()} deg",
            value = rotation,
            range = -180f..180f,
            onValueChange = { rotation = it }
        )
        TransformSliderCard(
            title = "Opacity",
            valueLabel = "${(opacity * 100).toInt()}%",
            value = opacity,
            range = 0f..1f,
            onValueChange = { opacity = it }
        )
        TransformSliderCard(
            title = "Freeze Frame",
            valueLabel = "${freezeDurationMs}ms",
            value = freezeDurationMs.toFloat(),
            range = 0f..3000f,
            onValueChange = { freezeDurationMs = it.toInt() }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    onApply(positionX, positionY, scaleX, scaleY, rotation, opacity, freezeDurationMs)
                },
                modifier = Modifier.weight(1f),
                enabled = clip != null
            ) {
                Text("Apply")
            }
            Button(
                onClick = onClose,
                modifier = Modifier.weight(1f)
            ) {
                Text("Close")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun TransformSliderCard(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(valueLabel, style = MaterialTheme.typography.bodySmall)
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
