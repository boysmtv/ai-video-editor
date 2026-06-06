package com.changecut.feature.editor.pro

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.changecut.core.editor.ColorGradeDef

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ColorGradingScreen(
    currentGrade: ColorGradeDef?,
    onApply: (ColorGradeDef?) -> Unit,
    onClose: () -> Unit
) {
    var hue by remember(currentGrade) { mutableFloatStateOf(currentGrade?.hslHue ?: 0f) }
    var saturation by remember(currentGrade) { mutableFloatStateOf(currentGrade?.hslSaturation ?: 0f) }
    var lightness by remember(currentGrade) { mutableFloatStateOf(currentGrade?.hslLightness ?: 0f) }
    var vignette by remember(currentGrade) { mutableFloatStateOf(currentGrade?.vignetteIntensity ?: 0f) }
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Color Grading") },
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
                            onApply(ColorGradeDef(hslHue = hue, hslSaturation = saturation, hslLightness = lightness, vignetteIntensity = vignette))
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
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("HSL", "Vignette").forEachIndexed { i, label ->
                    FilterChip(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        label = { Text(label) },
                        leadingIcon = { Icon(Icons.Default.ColorLens, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            when (selectedTab) {
                0 -> {
                    HSLSlider("Hue", hue, -180f..180f) { hue = it }
                    HSLSlider("Saturation", saturation, -100f..100f) { saturation = it }
                    HSLSlider("Lightness", lightness, -100f..100f) { lightness = it }
                }
                1 -> {
                    HSLSlider("Vignette", vignette, 0f..1f) { vignette = it }
                }
            }
        }
    }
}

@Composable
private fun HSLSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$label: ${value.toInt()}", modifier = Modifier.width(120.dp), style = MaterialTheme.typography.bodySmall)
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f))
    }
}
