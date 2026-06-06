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

import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.changecut.core.editor.AnimationCatalog
import com.changecut.core.editor.AnimationDef

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnimationPickerScreen(
    currentIn: AnimationDef?,
    currentOut: AnimationDef?,
    onApply: (AnimationDef?, AnimationDef?) -> Unit,
    onClose: () -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    var selectedIn by remember(currentIn, currentOut) { mutableStateOf(currentIn) }
    var selectedOut by remember(currentIn, currentOut) { mutableStateOf(currentOut) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Animations") },
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
                            .clickable { onApply(null, null) }
                    )
                    Button(
                        onClick = { onApply(selectedIn, selectedOut) },
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
                .padding(16.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = tabIndex == 0,
                    onClick = { tabIndex = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("In") }
                SegmentedButton(
                    selected = tabIndex == 1,
                    onClick = { tabIndex = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Out") }
            }

            Spacer(Modifier.height(16.dp))

            val animations = if (tabIndex == 0) AnimationCatalog.inAnimations else AnimationCatalog.outAnimations
            val selectedAnim = if (tabIndex == 0) selectedIn else selectedOut

            Text(
                text = if (tabIndex == 0) "In Animation" else "Out Animation",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                animations.forEach { anim ->
                    FilterChip(
                        selected = selectedAnim?.id == anim.id,
                        onClick = {
                            val newAnim = if (selectedAnim?.id == anim.id) null else anim
                            if (tabIndex == 0) selectedIn = newAnim else selectedOut = newAnim
                        },
                        label = { Text(anim.name, fontSize = 11.sp) },

                    )
                }
            }

            if (selectedAnim != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Selected: ${selectedAnim.name} (${selectedAnim.type.name})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
