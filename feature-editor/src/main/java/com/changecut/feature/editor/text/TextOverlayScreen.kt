package com.changecut.feature.editor.text

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BorderStyle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic

import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.changecut.core.editor.TextClipStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TextOverlayScreen(
    trackIndex: Int = 2,
    clipId: String? = null,
    initialText: String = "",
    initialStyle: TextClipStyle? = null,
    onCancel: () -> Unit,
    onApplied: () -> Unit,
    viewModel: TextOverlayViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(clipId, initialText, initialStyle) {
        viewModel.bindEditor(clipId = clipId, initialText = initialText, initialStyle = initialStyle)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Text Overlay") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.saveToTrack(trackIndex, clipId)
                        onApplied()
                    }) {
                        Icon(Icons.Default.Check, "Apply")
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
            TextPreviewBox(previewText = state.previewText, style = state)

            OutlinedTextField(
                value = state.textContent,
                onValueChange = { viewModel.setTextContent(it) },
                label = { Text("Enter text") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 4
            )

            FontSizeSlider(fontSize = state.fontSize, onSizeChange = { viewModel.setFontSize(it) })

            Text("Color", style = MaterialTheme.typography.labelMedium)
            ColorPickerGrid(selectedColor = state.color, onColorSelected = { viewModel.setColor(it) })

            Text("Alignment", style = MaterialTheme.typography.labelMedium)
            AlignmentSelector(selectedAlignment = state.alignment, onAlignmentSelected = { viewModel.setAlignment(it) })

            Text("Text Style", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.bold,
                    onClick = { viewModel.toggleBold() },
                    label = { Text("B") },
                    leadingIcon = { Icon(Icons.Default.FormatBold, "Bold", modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = state.italic,
                    onClick = { viewModel.toggleItalic() },
                    label = { Text("I") },
                    leadingIcon = { Icon(Icons.Default.FormatItalic, "Italic", modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = state.shadow,
                    onClick = { viewModel.toggleShadow() },
                    label = { Text("Shadow") }
                )
                FilterChip(
                    selected = state.outline,
                    onClick = { viewModel.toggleOutline() },
                    label = { Text("Outline") },
                    leadingIcon = { Icon(Icons.Default.BorderStyle, "Outline", modifier = Modifier.size(16.dp)) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        viewModel.saveToTrack(trackIndex, clipId)
                        onApplied()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply")
                }
            }
        }
    }
}

@Composable
private fun TextPreviewBox(previewText: String, style: TextOverlayUiState) {
    val textColor = Color(style.color)
    val bgColor = style.backgroundColor?.let { Color(it) } ?: Color(0xFF1A1A2E)
    val fontWeight = if (style.bold) FontWeight.Bold else FontWeight.Normal
    val fontStyle = if (style.italic) FontStyle.Italic else FontStyle.Normal
    val textAlign = when (style.alignment) {
        1 -> TextAlign.Center
        2 -> TextAlign.End
        else -> TextAlign.Center
    }
    val decoration = if (style.outline) TextDecoration.Underline else TextDecoration.None

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(bgColor, RoundedCornerShape(8.dp))
            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = previewText,
            style = TextStyle(
                fontSize = style.fontSize.sp,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                textAlign = textAlign,
                textDecoration = decoration
            ),
            color = textColor
        )
    }
}

@Composable
private fun FontSizeSlider(fontSize: Float, onSizeChange: (Float) -> Unit) {
    Column {
        Text("Font Size: ${fontSize.toInt()}sp", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = fontSize,
            onValueChange = onSizeChange,
            valueRange = 8f..120f,
            steps = 111,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPickerGrid(selectedColor: Long, onColorSelected: (Long) -> Unit) {
    val colors = listOf(
        0xFFFFFFFFL, 0xFF000000L, 0xFFFF0000L, 0xFF00FF00L,
        0xFF0000FFL, 0xFFFFFF00L, 0xFFFF00FFL, 0xFF00FFFFL,
        0xFFFF8800L, 0xFF8800FFL, 0xFF0088FFL, 0xFF00FF88L,
        0xFFFF4488L, 0xFF4488FFL, 0xFF88FF44L, 0xFFFF8844L,
        0xFF888888L, 0xFF444444L, 0xFFAA5522L, 0xFF2255AAL
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        colors.forEach { color ->
            val isSelected = color == selectedColor
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(color), CircleShape)
                    .then(
                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        else Modifier.border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                    )
                    .clickable { onColorSelected(color) }
            )
        }
    }
}

@Composable
private fun AlignmentSelector(selectedAlignment: Int, onAlignmentSelected: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        val alignments = listOf(
            0 to "Top",
            1 to "Center",
            2 to "Bottom",
            3 to "Left",
            4 to "Right"
        )
        alignments.forEach { (value, label) ->
            FilterChip(
                selected = selectedAlignment == value,
                onClick = { onAlignmentSelected(value) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}
