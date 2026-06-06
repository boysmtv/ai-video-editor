package com.changecut.feature.editor.pro

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Layers

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.changecut.core.editor.BlendMode
import com.changecut.core.editor.MaskDef
import com.changecut.core.editor.MaskType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MaskEditorScreen(
    initialMask: MaskDef?,
    initialBlendMode: BlendMode,
    onApply: (mask: MaskDef?, blendMode: BlendMode) -> Unit,
    onClose: () -> Unit
) {
    var selectedType by remember(initialMask, initialBlendMode) { mutableStateOf(initialMask?.type ?: MaskType.RECTANGLE) }
    var selectedBlend by remember(initialMask, initialBlendMode) { mutableStateOf(initialBlendMode) }
    var maskSize by remember(initialMask) { mutableFloatStateOf(initialMask?.width ?: 0.8f) }
    var feather by remember(initialMask) { mutableFloatStateOf(initialMask?.featherPx ?: 0f) }
    var invert by remember(initialMask) { mutableStateOf(initialMask?.invert ?: false) }
    val mask = remember(selectedType, maskSize, feather, invert) {
        MaskDef(
            type = selectedType,
            width = maskSize,
            height = maskSize,
            featherPx = feather,
            invert = invert
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mask & Blend") },
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
                            .clickable { onApply(null, selectedBlend) }
                    )
                    Button(
                        onClick = {
                            val finalMask = if (selectedType == MaskType.RECTANGLE && maskSize >= 1f) null
                            else mask
                            onApply(finalMask, selectedBlend)
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
            Text("Mask Shape", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MaskType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type.name, fontSize = MaterialTheme.typography.labelSmall.fontSize) },
                        leadingIcon = {
                            Icon(
                                imageVector = when (type) {
                                    MaskType.RECTANGLE -> Icons.Default.Layers
                                    MaskType.HEART -> Icons.Default.Favorite
                                    else -> Icons.Default.Layers
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                MaskPreview(
                    mask = mask,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Size: ${(maskSize * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = maskSize,
                    onValueChange = { maskSize = it },
                    valueRange = 0.1f..1.0f,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Feather: ${feather.toInt()}px", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = feather,
                    onValueChange = { feather = it },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Invert Mask", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(8.dp))
                androidx.compose.material3.Switch(
                    checked = invert,
                    onCheckedChange = { invert = it }
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("Blend Mode", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BlendMode.entries.forEach { mode ->
                    FilterChip(
                        selected = selectedBlend == mode,
                        onClick = { selectedBlend = mode },
                        label = { Text(mode.displayName, fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Layers,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MaskPreview(mask: MaskDef, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val canvasW = size.width
        val canvasH = size.height
        val cx = mask.centerX * canvasW
        val cy = mask.centerY * canvasH
        val mw = mask.width * canvasW / 2f
        val mh = mask.height * canvasH / 2f

        when (mask.type) {
            MaskType.RECTANGLE -> {
                drawRect(
                    color = Color.White.copy(alpha = 0.6f),
                    topLeft = Offset(cx - mw / 2f, cy - mh / 2f),
                    size = Size(mw, mh)
                )
                drawRect(
                    color = Color.White,
                    topLeft = Offset(cx - mw / 2f, cy - mh / 2f),
                    size = Size(mw, mh),
                    style = Stroke(width = 2f)
                )
            }
            MaskType.LINEAR -> {
                val angle = java.lang.Math.toRadians(mask.rotation.toDouble())
                val endX = (cx + java.lang.Math.cos(angle) * canvasW / 2f).toFloat()
                val endY = (cy + java.lang.Math.sin(-angle) * canvasH / 2f).toFloat()
                drawLine(
                    color = Color.White,
                    start = Offset(cx, cy),
                    end = Offset(endX, endY),
                    strokeWidth = 4f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.3f),
                    start = Offset(cx - (endX - cx), cy - (endY - cy)),
                    end = Offset(cx, cy),
                    strokeWidth = 4f
                )
            }
            MaskType.RADIAL -> {
                drawCircle(
                    color = Color.White.copy(alpha = 0.6f),
                    radius = mw / 2f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color.White,
                    radius = mw / 2f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2f)
                )
            }
            MaskType.HEART -> {
                val pts = 50
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx, cy - mh * 0.2f)
                    for (i in 0..pts) {
                        val t = i.toFloat() / pts * 2f * Math.PI.toFloat()
                        val st = sinF(t)
                        val ct = cosF(t)
                        val c2t = cosF(2f * t)
                        val c3t = cosF(3f * t)
                        val c4t = cosF(4f * t)
                        val x = 16f * st * st * st * mw / 40f
                        val y = -(13f * ct - 5f * c2t - 2f * c3t - c4t) * mh / 40f
                        lineTo(cx + x, cy + y)
                    }
                    close()
                }
                drawPath(path, color = Color.White.copy(alpha = 0.6f))
                drawPath(path, color = Color.White, style = Stroke(width = 2f))
            }
            MaskType.STAR -> {
                val pts = 5
                val path = androidx.compose.ui.graphics.Path().apply {
                    for (i in 0 until pts * 2) {
                        val r = if (i % 2 == 0) mw / 2f else mw / 4f
                        val angle = i.toFloat() / (pts * 2) * 2f * Math.PI.toFloat() - Math.PI.toFloat() / 2f
                        val x = cx + r * cosF(angle)
                        val y = cy + r * sinF(angle)
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(path, color = Color.White.copy(alpha = 0.6f))
                drawPath(path, color = Color.White, style = Stroke(width = 2f))
            }
            MaskType.CUSTOM -> {
                if (mask.points.isNotEmpty()) {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        mask.points.forEachIndexed { i, (px, py) ->
                            val x = px * canvasW
                            val y = py * canvasH
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        close()
                    }
                    drawPath(path, color = Color.White.copy(alpha = 0.6f))
                } else {
                    drawRect(
                        color = Color.White.copy(alpha = 0.3f),
                        topLeft = Offset.Zero,
                        size = size,
                        style = Stroke(width = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                    )
                    drawLine(Color.White.copy(alpha = 0.3f), Offset(cx, 0f), Offset(cx, canvasH), strokeWidth = 1f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f)))
                    drawLine(Color.White.copy(alpha = 0.3f), Offset(0f, cy), Offset(canvasW, cy), strokeWidth = 1f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f)))
                }
            }
        }

        if (mask.invert) {
            drawRect(
                color = Color.Red.copy(alpha = 0.2f),
                topLeft = Offset.Zero,
                size = size
            )
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun sinF(v: Float): Float = kotlin.math.sin(v.toDouble()).toFloat()
@Suppress("NOTHING_TO_INLINE")
private inline fun cosF(v: Float): Float = kotlin.math.cos(v.toDouble()).toFloat()
