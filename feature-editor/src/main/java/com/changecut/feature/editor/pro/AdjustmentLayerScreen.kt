package com.changecut.feature.editor.pro

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.changecut.core.editor.ClipGroup
import com.changecut.core.editor.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustmentLayerScreen(
    tracks: List<Track>,
    groups: List<ClipGroup>,
    selectedClipCount: Int,
    onAddGroup: () -> Unit,
    onAddAdjustmentTrack: () -> Unit,
    onClearSelection: () -> Unit,
    onSelectGroup: (String) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onToggleGroupExpand: (String) -> Unit,
    onClose: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adjustment & Groups") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    Button(onClick = onClose, modifier = Modifier.padding(end = 8.dp)) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
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
                .padding(16.dp)
        ) {
            Text("Tracks", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            tracks.forEach { track ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${track.type.name}: ${track.label}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        if (track.isVisible) "Visible" else "Hidden",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (track.isVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Clip Groups", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Button(onClick = onAddGroup) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Group")
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Selected clips: $selectedClipCount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onAddAdjustmentTrack) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Adjustment Track")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onClearSelection, enabled = selectedClipCount > 0) {
                    Text("Clear Selection")
                }
            }
            Spacer(Modifier.height(12.dp))

            if (groups.isEmpty()) {
                Text(
                    "No groups yet. Create a group to edit multiple clips together.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                groups.forEach { group ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    group.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    group.clipIds.joinToString(limit = 3, truncated = "..."),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(onClick = { onSelectGroup(group.id) }) {
                                Text("Select")
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${group.clipIds.size} clips",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = group.isExpanded,
                                onCheckedChange = { onToggleGroupExpand(group.id) }
                            )
                            IconButton(onClick = { onDeleteGroup(group.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete group")
                            }
                        }
                    }
                }
            }
        }
    }
}
