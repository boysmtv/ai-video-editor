package com.changecut.feature.editor.audio

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.changecut.core.editor.EditorClip
import com.changecut.core.editor.SnapshotCommand
import com.changecut.core.editor.TrackManager
import com.changecut.core.editor.TrackType
import com.changecut.core.editor.UndoRedoManager
import com.changecut.core.ffmpeg.FfmpegExecutor
import com.changecut.core.media.MediaUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class AudioTrackInfo(
    val id: String,
    val name: String,
    val uri: String,
    val durationMs: Long = 0L,
    val volume: Float = 1.0f,
    val fadeInMs: Int = 0,
    val fadeOutMs: Int = 0,
    val startOffsetUs: Long = 0L,
    val isRecording: Boolean = false
)

data class AudioTrackUiState(
    val audioTracks: List<AudioTrackInfo> = emptyList(),
    val isRecording: Boolean = false,
    val recordingAmplitude: Float = 0f,
    val error: String? = null
)

@HiltViewModel
class AudioTrackViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackManager: TrackManager,
    private val mediaUtils: MediaUtils,
    private val ffmpegExecutor: FfmpegExecutor,
    private val undoRedoManager: UndoRedoManager
) : ViewModel() {

    private val _state = MutableStateFlow(AudioTrackUiState())
    val state: StateFlow<AudioTrackUiState> = _state.asStateFlow()
    private var mediaRecorder: MediaRecorder? = null
    private var pendingRecordingPath: String? = null
    private var pendingRecordingName: String = "Voice Over"

    init {
        syncFromTimeline()
        viewModelScope.launch {
            trackManager.tracks.collect {
                syncFromTimeline()
            }
        }
    }

    fun importAudio(uri: Uri) {
        viewModelScope.launch {
            try {
                val sourceName = mediaUtils.queryMediaFromUri(uri)?.name
                    ?.substringBeforeLast('.')
                    ?.takeIf { it.isNotBlank() }
                    ?: "Audio ${_state.value.audioTracks.size + 1}"
                val fileName = mediaUtils.buildImportedFileName(
                    uri = uri,
                    prefix = "audio",
                    mimeType = context.contentResolver.getType(uri) ?: "audio/mpeg",
                    fallbackExtension = "mp3"
                )
                val copiedFile = mediaUtils.copyToCache(uri, fileName)
                if (copiedFile == null) {
                    _state.update { it.copy(error = "Failed to import audio") }
                    return@launch
                }
                val audioDir = File(context.filesDir, "audio_tracks")
                audioDir.mkdirs()
                val finalFile = File(audioDir, fileName)
                val storedFile = moveAudioFile(copiedFile, finalFile)
                if (storedFile == null) {
                    copiedFile.delete()
                    _state.update { it.copy(error = "Failed to store audio") }
                    return@launch
                }
                val trackId = UUID.randomUUID().toString()
                val durationMs = getAudioDuration(storedFile.absolutePath)

                val newTrack = AudioTrackInfo(
                    id = trackId,
                    name = sourceName,
                    uri = storedFile.absolutePath,
                    durationMs = durationMs
                )
                _state.update { it.copy(audioTracks = it.audioTracks + newTrack) }
                addAudioClipToTimeline(trackId, storedFile.absolutePath, sourceName, durationMs)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Import failed") }
            }
        }
    }

    fun recordVoiceOver() {
        try {
            val outputDir = File(context.filesDir, "audio_tracks/recordings")
            outputDir.mkdirs()
            val file = File(outputDir, "voiceover_${UUID.randomUUID()}.m4a")
            pendingRecordingPath = file.absolutePath
            pendingRecordingName = "Voice Over ${_state.value.audioTracks.size + 1}"

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            _state.update { it.copy(isRecording = true, recordingAmplitude = 0f, error = null) }
        } catch (e: Exception) {
            mediaRecorder?.release()
            mediaRecorder = null
            pendingRecordingPath = null
            _state.update { it.copy(isRecording = false, error = e.message ?: "Failed to start recording") }
        }
    }

    fun stopRecording() {
        val recorder = mediaRecorder ?: run {
            _state.update { it.copy(isRecording = false) }
            return
        }
        val outputPath = pendingRecordingPath
        mediaRecorder = null
        pendingRecordingPath = null
        try {
            recorder.stop()
        } catch (_: Exception) {
        } finally {
            recorder.release()
        }

        if (outputPath.isNullOrBlank() || !File(outputPath).exists()) {
            _state.update { it.copy(isRecording = false, error = "Recording output missing") }
            return
        }

        val trackId = UUID.randomUUID().toString()
        val durationMs = getAudioDuration(outputPath)
        val newTrack = AudioTrackInfo(
            id = trackId,
            name = pendingRecordingName,
            uri = outputPath,
            durationMs = durationMs
        )
        _state.update {
            it.copy(
                audioTracks = it.audioTracks + newTrack,
                isRecording = false,
                recordingAmplitude = 0f
            )
        }
        addAudioClipToTimeline(trackId, outputPath, newTrack.name, durationMs)
    }

    fun setVolume(trackId: String, volume: Float) {
        val clamped = volume.coerceIn(0f, 2f)
        _state.update { state ->
            state.copy(audioTracks = state.audioTracks.map {
                if (it.id == trackId) it.copy(volume = clamped) else it
            })
        }
        updateTimelineAudioClip(trackId) { it.copy(volume = clamped) }
    }

    fun setFadeIn(trackId: String, durationMs: Int) {
        _state.update { state ->
            state.copy(audioTracks = state.audioTracks.map {
                if (it.id == trackId) it.copy(fadeInMs = durationMs.coerceAtLeast(0)) else it
            })
        }
        updateTimelineAudioClip(trackId) { it.copy(audioFadeInMs = durationMs.coerceAtLeast(0)) }
    }

    fun setFadeOut(trackId: String, durationMs: Int) {
        _state.update { state ->
            state.copy(audioTracks = state.audioTracks.map {
                if (it.id == trackId) it.copy(fadeOutMs = durationMs.coerceAtLeast(0)) else it
            })
        }
        updateTimelineAudioClip(trackId) { it.copy(audioFadeOutMs = durationMs.coerceAtLeast(0)) }
    }

    fun removeTrack(trackId: String) {
        _state.update { state ->
            state.copy(audioTracks = state.audioTracks.filter { it.id != trackId })
        }
        val audioTrackIndex = trackManager.tracks.value.indexOfFirst { it.type == TrackType.AUDIO }
        if (audioTrackIndex >= 0) {
            undoRedoManager.execute(
                SnapshotCommand(trackManager, "Remove audio track") {
                    trackManager.removeClip(audioTrackIndex, trackId)
                }
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun getAudioDuration(path: String): Long {
        return ffmpegExecutor.getMediaInfo(path)?.durationMs ?: 0L
    }

    private fun syncFromTimeline() {
        val audioTracks = trackManager.tracks.value
            .withIndex()
            .filter { (_, track) -> track.type == TrackType.AUDIO }
            .flatMap { (trackIndex, track) ->
                track.clips.map { clip ->
                    AudioTrackInfo(
                        id = clip.id,
                        name = clip.label,
                        uri = clip.sourceUri,
                        durationMs = (clip.endOffsetUs - clip.startOffsetUs) / 1000L,
                        volume = clip.volume,
                        fadeInMs = clip.audioFadeInMs,
                        fadeOutMs = clip.audioFadeOutMs,
                        startOffsetUs = clip.startOffsetUs
                    ) to trackIndex
                }
            }
            .sortedWith(
                compareBy<Pair<AudioTrackInfo, Int>> { it.first.startOffsetUs }
                    .thenBy { it.second }
                    .thenBy { it.first.name.lowercase() }
            )
            .map { it.first }
        _state.update { it.copy(audioTracks = audioTracks) }
    }

    private fun addAudioClipToTimeline(
        clipId: String,
        path: String,
        label: String,
        durationMs: Long
    ) {
        val durationUs = durationMs * 1000L
        undoRedoManager.execute(
            SnapshotCommand(trackManager, "Add audio clip") {
                var audioTrackIndex = trackManager.tracks.value.indexOfFirst { it.type == TrackType.AUDIO }
                if (audioTrackIndex < 0) {
                    trackManager.addTrack(TrackType.AUDIO, "Audio")
                    audioTrackIndex = trackManager.tracks.value.indexOfFirst { it.type == TrackType.AUDIO }
                }
                if (audioTrackIndex < 0) return@SnapshotCommand
                val track = trackManager.tracks.value[audioTrackIndex]
                val startOffsetUs = track.clips.maxOfOrNull { it.endOffsetUs } ?: 0L
                trackManager.addClip(
                    audioTrackIndex,
                    EditorClip(
                        id = clipId,
                        sourceUri = path,
                        label = label,
                        startOffsetUs = startOffsetUs,
                        endOffsetUs = startOffsetUs + durationUs,
                        volume = 1f,
                        audioFadeInMs = 0,
                        audioFadeOutMs = 0
                    )
                )
                trackManager.selectTrack(audioTrackIndex)
                trackManager.selectClip(clipId)
            }
        )
    }

    private fun updateTimelineAudioClip(
        clipId: String,
        transform: (EditorClip) -> EditorClip
    ) {
        val tracks = trackManager.tracks.value
        for ((index, track) in tracks.withIndex()) {
            val clip = track.clips.find { it.id == clipId } ?: continue
            undoRedoManager.execute(
                SnapshotCommand(trackManager, "Update audio clip") {
                    trackManager.updateClip(index, transform(clip))
                }
            )
            return
        }
    }

    private fun moveAudioFile(source: File, target: File): File? {
        return try {
            target.parentFile?.mkdirs()
            if (target.exists()) target.delete()
            val moved = if (source.absolutePath == target.absolutePath) true else source.renameTo(target)
            if (moved) {
                target
            } else {
                source.copyTo(target, overwrite = true)
                source.delete()
                target
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {
        }
        mediaRecorder = null
    }
}
