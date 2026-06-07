package com.changecut.feature.editor.ai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiCaptionScreen(
    videoDescription: String = "",
    onNavigateBack: () -> Unit,
    onDone: () -> Unit,
    onImportCaption: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Caption", "Hashtags", "Hook")

    val captionEngine = remember { AiCaptionEngine() }

    var caption by remember { mutableStateOf("") }
    var hashtags by remember { mutableStateOf(listOf<String>()) }
    var hooks by remember { mutableStateOf(listOf<String>()) }
    var selectedHook by remember { mutableStateOf<String?>(null) }
    var editingCaption by remember { mutableStateOf(false) }
    var editableCaptionText by remember { mutableStateOf("") }

    LaunchedEffect(videoDescription) {
        caption = ""
        hashtags = emptyList()
        hooks = emptyList()
        selectedHook = null
        editingCaption = false
        editableCaptionText = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Content Tools") },
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
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> CaptionTab(
                        caption = caption,
                        editingCaption = editingCaption,
                        editableCaptionText = editableCaptionText,
                        onStartEdit = {
                            editingCaption = true
                            editableCaptionText = caption
                        },
                        onCaptionTextChange = { editableCaptionText = it },
                        onSaveEdit = {
                            caption = editableCaptionText
                            editingCaption = false
                        },
                        onCancelEdit = { editingCaption = false },
                        onGenerate = {
                            caption = captionEngine.generateCaption(videoDescription)
                            editableCaptionText = caption
                        },
                        onCopy = {
                            copyToClipboard(context, "caption", caption)
                        },
                        onImportCaption = onImportCaption,
                        modifier = Modifier.fillMaxWidth()
                    )
                    1 -> HashtagsTab(
                        hashtags = hashtags,
                        onGenerate = {
                            hashtags = captionEngine.generateHashtags(videoDescription)
                        },
                        onToggleHashtag = { tag ->
                            hashtags = if (tag in hashtags) {
                                hashtags - tag
                            } else {
                                hashtags + tag
                            }
                        },
                        onCopy = {
                            copyToClipboard(context, "hashtags", hashtags.joinToString(" "))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    2 -> HookTab(
                        hooks = hooks,
                        selectedHook = selectedHook,
                        onGenerate = {
                            hooks = captionEngine.generateHooks(videoDescription)
                        },
                        onSelectHook = { selectedHook = it },
                        onCopy = {
                            selectedHook?.let { hook ->
                                copyToClipboard(context, "hook", hook)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun CaptionTab(
    caption: String,
    editingCaption: Boolean,
    editableCaptionText: String,
    onStartEdit: () -> Unit,
    onCaptionTextChange: (String) -> Unit,
    onSaveEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onGenerate: () -> Unit,
    onCopy: () -> Unit,
    onImportCaption: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "AI Caption Generator",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (caption.isEmpty() || editingCaption) {
            OutlinedTextField(
                value = editableCaptionText,
                onValueChange = onCaptionTextChange,
                label = { Text("Edit caption") },
                placeholder = { Text("Your AI-generated caption will appear here") },
                minLines = 4,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth()
            )

            if (editingCaption) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSaveEdit) {
                        Text("Save")
                    }
                    TextButton(onClick = onCancelEdit) {
                        Text("Cancel")
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = caption, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onStartEdit) {
                            Text("Edit")
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onGenerate,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Generate Caption")
            }
            if (caption.isNotEmpty()) {
                OutlinedButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copy")
                }
                OutlinedButton(onClick = { onImportCaption(caption) }) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("To Timeline")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HashtagsTab(
    hashtags: List<String>,
    onGenerate: () -> Unit,
    onToggleHashtag: (String) -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Trending Hashtags",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (hashtags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                hashtags.forEach { tag ->
                    AssistChip(
                        onClick = { onToggleHashtag(tag) },
                        label = { Text(tag) },
                        leadingIcon = {
                            if (tag in hashtags) {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (tag in hashtags)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            OutlinedButton(
                onClick = onCopy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Copy All Hashtags")
            }
        }

        Button(
            onClick = onGenerate,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Generate Hashtags")
        }
    }
}

@Composable
private fun HookTab(
    hooks: List<String>,
    selectedHook: String?,
    onGenerate: () -> Unit,
    onSelectHook: (String) -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Attention-Grabbing Hooks",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (hooks.isNotEmpty()) {
            hooks.forEach { hook ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (hook == selectedHook) Modifier.padding(0.dp)
                            else Modifier
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hook == selectedHook)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    onClick = { onSelectHook(hook) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = hook,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (hook == selectedHook) {
                            Icon(
                                Icons.Default.Check,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (selectedHook != null) {
                OutlinedButton(
                    onClick = onCopy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copy Selected Hook")
                }
            }
        }

        Button(
            onClick = onGenerate,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Generate Hooks")
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}

private class AiCaptionEngine {
    private val captionTemplates = listOf(
        "This video is everything you need to see today \uD83D\uDD25\n\n" +
                "We spent hours perfecting this just for you. Every frame, every moment " +
                "was carefully selected to bring you the best experience.\n\n" +
                "Drop a \u2764\ufe0f if you enjoyed this!",
        "You won't believe what happened when we captured this \uD83D\uDE2E\n\n" +
                "Sometimes the best moments are the ones you least expect. " +
                "This is one of those moments that you have to see to believe.\n\n" +
                "Save this for later \uD83D\uDCCD",
        "The quality you've been waiting for \uD83C\uDFA5\u2728\n\n" +
                "Cinematic visuals, perfect timing, and a story that speaks for itself. " +
                "This is content creation at its finest.\n\n" +
                "Tag someone who needs to see this \uD83D\uDC4B",
        "POV: You just found the best video of the day \uD83C\uDF1F\n\n" +
                "From start to finish, every second of this video was crafted with " +
                "passion and precision. Watch till the end \u2014 you won't regret it.\n\n" +
                "Follow for more content like this!",
        "This is why we do what we do \uD83D\uDE4C\n\n" +
                "Creating content that inspires, entertains, and connects people. " +
                "This video represents countless hours of hard work and dedication.\n\n" +
                "Share this with someone who needs inspiration today"
    )

    private val hashtagSets = listOf(
        listOf("#viral", "#fyp", "#trending", "#viralvideo", "#foryou", "#foryoupage", "#explore"),
        listOf("#cinematic", "#videography", "#filmmaking", "#cinematography", "#director", "#bts"),
        listOf("#tiktok", "#reels", "#shorts", "#instagram", "#contentcreator", "#socialmedia"),
        listOf("#vlog", "#dailyvlog", "#lifestyle", "#travel", "#adventure", "#explorepage"),
        listOf("#edit", "#videoedit", "#capcut", "#editing", "#videoeditor", "#transition"),
        listOf("#motivation", "#inspiration", "#mindset", "#success", "#goal", "#hustle"),
        listOf("#funny", "#comedy", "#humor", "#lol", "#memes", "#laugh"),
        listOf("#beautiful", "#nature", "#photography", "#art", "#creative", "#instagood"),
        listOf("#tutorial", "#howto", "#tips", "#hacks", "#lifehack", "#diy"),
        listOf("#music", "#song", "#audio", "#sound", "#vibes", "#playlist")
    )

    private val hookTemplates = listOf(
        "Stop scrolling! You need to see this \uD83D\uDD25",
        "I can't believe this actually worked...",
        "Wait for the end \uD83D\uDE2E",
        "This changes EVERYTHING",
        "You've been doing it wrong this whole time",
        "The internet is obsessed with this \uD83D\uDC40",
        "I wish I knew this sooner \uD83D\uDE2D",
        "Only 1% of people know this secret",
        "This will blow your mind \uD83E\uDD2F",
        "Real or fake? You decide \uD83E\uDD14",
        "The most satisfying thing you'll see today \u2728",
        "I tried this for 7 days and here's what happened \uD83D\uDE33",
        "Nobody talks about this, but it's incredible \uD83D\uDE31",
        "You won't believe what happens next \uD83D\uDC40",
        "This video will make you see things differently \uD83E\uDDD8"
    )

    fun generateCaption(description: String): String {
        val index = kotlin.math.abs(description.hashCode()) % captionTemplates.size
        return captionTemplates[index]
    }

    fun generateHashtags(description: String): List<String> {
        val count = 8
        val selected = mutableSetOf<String>()

        val descWords = description.lowercase().split("\\s+".toRegex())
        for (set in hashtagSets) {
            for (tag in set) {
                val tagWord = tag.removePrefix("#").lowercase()
                if (descWords.any { it.contains(tagWord) }) {
                    selected.add(tag)
                    if (selected.size >= count) break
                }
            }
            if (selected.size >= count) break
        }

        if (selected.size < count) {
            for (set in hashtagSets.shuffled()) {
                for (tag in set) {
                    if (selected.size >= count) break
                    selected.add(tag)
                }
            }
        }

        return selected.shuffled().take(count)
    }

    fun generateHooks(description: String): List<String> {
        return hookTemplates.shuffled().take(5)
    }
}
