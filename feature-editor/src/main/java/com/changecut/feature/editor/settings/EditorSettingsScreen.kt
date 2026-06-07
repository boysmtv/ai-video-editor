package com.changecut.feature.editor.settings

import android.content.pm.PackageManager
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.changecut.core.cache.CacheCleaner
import com.changecut.core.export.ExportPreset
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorSettingsScreen(
    onNavigateBack: () -> Unit,
    isDarkTheme: Boolean = false,
    onThemeChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cacheCleaner = remember { CacheCleaner(context) }

    var selectedPreset by remember { mutableStateOf(ExportPreset.FULL_HD) }
    var selectedFps by remember { mutableIntStateOf(30) }
    var selectedAspectRatio by remember { mutableStateOf("9:16") }
    var autoSaveEnabled by remember { mutableStateOf(true) }
    var cacheSize by remember { mutableStateOf("Calculating...") }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var isCleaning by remember { mutableStateOf(false) }

    val aspectRatios = listOf("9:16", "16:9", "1:1", "4:3")
    val fpsOptions = listOf(24, 30, 60)

    LaunchedEffect(Unit) {
        val size = withContext(Dispatchers.IO) {
            cacheCleaner.getCacheSize()
        }
        cacheSize = cacheCleaner.formatSize(size)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SectionHeader("Export Quality")

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExportPreset.allPresets.forEach { preset ->
                    FilterChip(
                        selected = selectedPreset.name == preset.name,
                        onClick = { selectedPreset = preset },
                        label = {
                            Column {
                                Text(preset.name, fontWeight = FontWeight.Medium)
                                Text(
                                    "${preset.width}x${preset.height} | ${preset.bitrate}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            HorizontalDivider()

            SectionHeader("Default FPS")

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                fpsOptions.forEach { fps ->
                    FilterChip(
                        selected = selectedFps == fps,
                        onClick = { selectedFps = fps },
                        label = { Text("${fps} fps") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            HorizontalDivider()

            SectionHeader("Default Aspect Ratio")

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                aspectRatios.forEach { ratio ->
                    FilterChip(
                        selected = selectedAspectRatio == ratio,
                        onClick = { selectedAspectRatio = ratio },
                        label = { Text(ratio) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            HorizontalDivider()

            SectionHeader("Appearance")

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Dark Theme", fontWeight = FontWeight.Medium)
                    Text(
                        "Use a darker editor surface",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = onThemeChange
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Auto-Save", fontWeight = FontWeight.Medium)
                    Text(
                        "Automatically save project changes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoSaveEnabled,
                    onCheckedChange = { autoSaveEnabled = it }
                )
            }

            HorizontalDivider()

            SectionHeader("Cache Management")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Storage,
                                null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Cache Size", fontWeight = FontWeight.Medium)
                        }
                        Text(
                            text = cacheSize,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { showClearCacheDialog = true },
                        enabled = !isCleaning,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (isCleaning) "Cleaning..." else "Clear All Cache")
                    }
                }
            }

            HorizontalDivider()

            SectionHeader("App Info")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val versionName = try {
                        context.packageManager.getPackageInfo(
                            context.packageName, 0
                        ).versionName ?: "1.0.0"
                    } catch (_: Exception) {
                        "1.0.0"
                    }

                    val versionCode = try {
                        context.packageManager.getPackageInfo(
                            context.packageName, 0
                        ).longVersionCode
                    } catch (_: Exception) {
                        1L
                    }

                    AppInfoRow("App Name", "ChangeCut")
                    AppInfoRow("Version", "v$versionName ($versionCode)")
                    AppInfoRow("Developer", "ChangeCut Team")

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showLicensesDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Info,
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Open Source Licenses")
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Cache") },
            text = { Text("This will delete all temporary files including media cache, thumbnails, and voice overs. Project files will not be affected.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCacheDialog = false
                        isCleaning = true
                        scope.launch(Dispatchers.IO) {
                            cacheCleaner.cleanAllCache()
                            withContext(Dispatchers.Main) {
                                cacheSize = "0 B"
                                isCleaning = false
                                Toast.makeText(
                                    context,
                                    "Cache cleared successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            title = { Text("Open Source Licenses") },
            text = {
                Column {
                    Text("This application uses the following open source libraries:")
                    Spacer(Modifier.height(8.dp))
                    Text("\u2022 FFmpegKit (GPL v3)")
                    Text("\u2022 Kotlinx Serialization (Apache 2.0)")
                    Text("\u2022 Hilt/Dagger (Apache 2.0)")
                    Text("\u2022 Jetpack Compose (Apache 2.0)")
                    Text("\u2022 Material3 (Apache 2.0)")
                    Text("\u2022 Room Database (Apache 2.0)")
                    Text("\u2022 Kotlin Coroutines (Apache 2.0)")
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicensesDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun AppInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
