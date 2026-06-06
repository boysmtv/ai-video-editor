package com.changecut.feature.editor.subtitle

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.changecut.core.ffmpeg.FfmpegExecutor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class SubtitleItem(
    val id: String,
    val startUs: Long,
    val endUs: Long,
    val text: String
) {
    val durationUs: Long get() = (endUs - startUs).coerceAtLeast(0L)
}

data class SubtitleUiState(
    val subtitles: List<SubtitleItem> = emptyList(),
    val currentTimeUs: Long = 0L,
    val isAutoGenerating: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SubtitleViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ffmpegExecutor: FfmpegExecutor
) : ViewModel() {

    private val _state = MutableStateFlow(SubtitleUiState())
    val state: StateFlow<SubtitleUiState> = _state.asStateFlow()

    fun setCurrentTime(timeUs: Long) {
        _state.update { it.copy(currentTimeUs = timeUs) }
    }

    fun addSubtitle(startUs: Long, endUs: Long, text: String) {
        if (text.isBlank() || startUs >= endUs) return
        val item = SubtitleItem(
            id = UUID.randomUUID().toString(),
            startUs = startUs,
            endUs = endUs,
            text = text
        )
        _state.update { state ->
            state.copy(subtitles = (state.subtitles + item).sortedBy { it.startUs })
        }
    }

    fun addSubtitleAtCurrentTime(text: String) {
        val currentTime = _state.value.currentTimeUs
        addSubtitle(currentTime, currentTime + 3_000_000, text)
    }

    fun updateSubtitle(id: String, text: String) {
        _state.update { state ->
            state.copy(subtitles = state.subtitles.map {
                if (it.id == id) it.copy(text = text) else it
            })
        }
    }

    fun updateSubtitleTime(id: String, startUs: Long, endUs: Long) {
        _state.update { state ->
            state.copy(
                subtitles = state.subtitles
                    .map {
                        if (it.id == id) it.copy(startUs = startUs, endUs = endUs) else it
                    }
                    .sortedBy { it.startUs }
            )
        }
    }

    fun removeSubtitle(id: String) {
        _state.update { state ->
            state.copy(subtitles = state.subtitles.filter { it.id != id })
        }
    }

    fun setSubtitles(items: List<SubtitleItem>) {
        _state.update {
            it.copy(subtitles = items.sortedBy { item -> item.startUs })
        }
    }

    fun parseSRT(content: String) {
        val items = mutableListOf<SubtitleItem>()
        val blocks = content.trim().split(Regex("\\n\\s*\\n"))
        for (block in blocks) {
            val lines = block.trim().lines()
            if (lines.size < 3) continue
            val timeLine = lines.getOrNull(1) ?: continue
            val timeMatch = Regex("(\\d{2}):(\\d{2}):(\\d{2})[,\\.](\\d{3})\\s*-->\\s*(\\d{2}):(\\d{2}):(\\d{2})[,\\.](\\d{3})")
                .find(timeLine) ?: continue
            val startUs = parseSrtTime(
                timeMatch.groupValues[1],
                timeMatch.groupValues[2],
                timeMatch.groupValues[3],
                timeMatch.groupValues[4]
            )
            val endUs = parseSrtTime(
                timeMatch.groupValues[5],
                timeMatch.groupValues[6],
                timeMatch.groupValues[7],
                timeMatch.groupValues[8]
            )
            val text = lines.drop(2).joinToString("\n").trim()
            if (text.isNotBlank()) {
                items.add(SubtitleItem(
                    id = UUID.randomUUID().toString(),
                    startUs = startUs,
                    endUs = endUs,
                    text = text
                ))
            }
        }
        _state.update { it.copy(subtitles = items.sortedBy { s -> s.startUs }) }
    }

    fun exportSRT(): String {
        val sb = StringBuilder()
        _state.value.subtitles.sortedBy { it.startUs }.forEachIndexed { index, item ->
            val indexStr = (index + 1).toString()
            val startStr = formatSrtTime(item.startUs)
            val endStr = formatSrtTime(item.endUs)
            sb.appendLine(indexStr)
            sb.appendLine("$startStr --> $endStr")
            sb.appendLine(item.text)
            sb.appendLine()
        }
        return sb.toString().trimEnd()
    }

    fun autoGenerateFromAudio(audioPath: String) {
        _state.update { it.copy(isAutoGenerating = true) }
        viewModelScope.launch {
            try {
                val command = ffmpegExecutor.buildCommand(
                    "-i", audioPath,
                    "-vn",
                    "-af", "silencedetect=noise=-30dB:d=0.5",
                    "-f", "null",
                    "-"
                )
                ffmpegExecutor.execute(command)
                _state.update { it.copy(isAutoGenerating = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isAutoGenerating = false, error = e.message) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun parseSrtTime(h: String, m: String, s: String, ms: String): Long {
        val hours = h.toLongOrNull() ?: 0L
        val minutes = m.toLongOrNull() ?: 0L
        val seconds = s.toLongOrNull() ?: 0L
        val millis = ms.toLongOrNull() ?: 0L
        return ((hours * 3600 + minutes * 60 + seconds) * 1_000_000 + millis * 1000)
    }

    private fun formatSrtTime(us: Long): String {
        val totalMs = us / 1000
        val h = totalMs / 3600000
        val m = (totalMs % 3600000) / 60000
        val s = (totalMs % 60000) / 1000
        val ms = totalMs % 1000
        return String.format("%02d:%02d:%02d,%03d", h, m, s, ms)
    }
}
