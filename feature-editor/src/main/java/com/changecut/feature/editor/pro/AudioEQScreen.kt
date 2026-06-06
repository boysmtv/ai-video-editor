package com.changecut.feature.editor.pro

import androidx.compose.foundation.clickable
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

import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.changecut.core.editor.AudioEQDef

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEQScreen(
    currentEQ: AudioEQDef?,
    onApply: (AudioEQDef?) -> Unit,
    onClose: () -> Unit
) {
    var lowGain by remember(currentEQ) { mutableFloatStateOf(currentEQ?.lowGain ?: 0f) }
    var midGain by remember(currentEQ) { mutableFloatStateOf(currentEQ?.midGain ?: 0f) }
    var highGain by remember(currentEQ) { mutableFloatStateOf(currentEQ?.highGain ?: 0f) }
    var compThreshold by remember(currentEQ) { mutableFloatStateOf(currentEQ?.compressorThreshold ?: -20f) }
    var compRatio by remember(currentEQ) { mutableFloatStateOf(currentEQ?.compressorRatio ?: 1f) }
    var duckAmount by remember(currentEQ) { mutableFloatStateOf(currentEQ?.duckingAmount ?: 0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio EQ") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    Text(
                        text = "Clear",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { onApply(null) }
                    )
                    Button(
                        onClick = {
                            onApply(AudioEQDef(lowGain = lowGain, midGain = midGain, highGain = highGain, compressorThreshold = compThreshold, compressorRatio = compRatio, duckingAmount = duckAmount))
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
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
                .padding(16.dp)
        ) {
            Text("Equalizer", style = MaterialTheme.typography.titleSmall)
            EQSlider("Low (250Hz)", lowGain, -20f..20f) { lowGain = it }
            EQSlider("Mid (1kHz)", midGain, -20f..20f) { midGain = it }
            EQSlider("High (8kHz)", highGain, -20f..20f) { highGain = it }

            Spacer(Modifier.height(16.dp))
            Text("Compressor", style = MaterialTheme.typography.titleSmall)
            EQSlider("Threshold", compThreshold, -60f..0f) { compThreshold = it }
            EQSlider("Ratio", compRatio, 1f..20f) { compRatio = it }

            Spacer(Modifier.height(16.dp))
            Text("Audio Ducking", style = MaterialTheme.typography.titleSmall)
            EQSlider("Duck Amount", duckAmount, 0f..1f) { duckAmount = it }
        }
    }
}

@Composable
private fun EQSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$label: ${"%.1f".format(value)}", modifier = Modifier.width(140.dp), style = MaterialTheme.typography.bodySmall)
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f))
    }
}
