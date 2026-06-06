package com.changecut.feature.editor.effects

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.changecut.core.editor.EffectCategory
import com.changecut.core.editor.EffectDef

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EffectScreen(
    clipId: String?,
    onNavigateBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: EffectViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(clipId) {
        viewModel.setCurrentClip(clipId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Effects") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onDone) {
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
            if (state.currentClipId == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Select a visual clip to apply effects.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
                return@Scaffold
            }

            CategoryTabs(
                selectedCategory = state.selectedCategory,
                onCategorySelected = { viewModel.selectCategory(it) },
                modifier = Modifier.fillMaxWidth()
            )

            if (state.currentClipId != null && state.appliedEffectId != null) {
                Button(
                    onClick = { state.currentClipId?.let { viewModel.removeEffect(it) } },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text("Remove Effect")
                }
            }

            if (state.currentClipId != null && state.appliedFilterId != null) {
                Button(
                    onClick = { state.currentClipId?.let { viewModel.removeFilter(it) } },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text("Remove Filter")
                }
            }

            EffectGrid(
                effects = state.categoryEffects,
                activeEffectId = state.appliedEffectId ?: state.appliedFilterId,
                onEffectClick = { effect ->
                    val clip = state.currentClipId ?: return@EffectGrid
                    if (effect.category == EffectCategory.COLOR) {
                        viewModel.applyFilter(clip, effect.id)
                    } else {
                        viewModel.applyEffect(clip, effect.id)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            )
        }
    }
}

@Composable
private fun CategoryTabs(
    selectedCategory: EffectCategory,
    onCategorySelected: (EffectCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(EffectCategory.entries) { category ->
            FilterChip(
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                label = { Text(category.name.lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}

@Composable
private fun EffectGrid(
    effects: List<EffectDef>,
    activeEffectId: String?,
    onEffectClick: (EffectDef) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(effects, key = { it.id }) { effect ->
            EffectThumbnail(
                effect = effect,
                isActive = effect.id == activeEffectId,
                onClick = { onEffectClick(effect) }
            )
        }
    }
}

@Composable
private fun EffectThumbnail(
    effect: EffectDef,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val border = if (isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        border = border,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = if (isActive) 8.dp else 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = thumbnailColor(effect.category),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = effect.name.take(2),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        }
                    }
                }
                Text(
                    text = effect.name,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

private fun thumbnailColor(category: EffectCategory): Color = when (category) {
    EffectCategory.COLOR -> Color(0xFF6C63FF)
    EffectCategory.BLUR -> Color(0xFF9C27B0)
    EffectCategory.GLITCH -> Color(0xFFFF5722)
    EffectCategory.SHATTER -> Color(0xFF795548)
    EffectCategory.SHAKE -> Color(0xFFFF9800)
    EffectCategory.ZOOM -> Color(0xFF4CAF50)
    EffectCategory.BLEND -> Color(0xFF03A9F4)
}
