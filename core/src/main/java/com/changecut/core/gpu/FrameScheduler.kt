package com.changecut.core.gpu

import android.os.SystemClock
import com.changecut.core.editor.EditorClip
import com.changecut.core.editor.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FrameScheduler @Inject constructor() {
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private val _currentRenderTimeUs = MutableStateFlow(0L)
    val currentRenderTimeUs: StateFlow<Long> = _currentRenderTimeUs.asStateFlow()

    private var playbackJob: Job? = null
    private var isPlaying = false
    private var playStartRealMs = 0L
    private var playStartTimeUs = 0L
    private var speedMultiplier = 1.0f
    private var durationUs = 0L

    private val audioDecoders = mutableMapOf<String, com.changecut.core.gpu.VideoFrameDecoder>()

    fun play() {
        if (isPlaying) return
        isPlaying = true
        playStartRealMs = SystemClock.elapsedRealtime()
        playStartTimeUs = _currentRenderTimeUs.value

        playbackJob?.cancel()
        playbackJob = scope.launch {
            while (isActive && isPlaying) {
                val elapsedMs = (SystemClock.elapsedRealtime() - playStartRealMs)
                val newTimeUs = (playStartTimeUs + (elapsedMs * 1000L * speedMultiplier).toLong())
                    .coerceIn(0L, durationUs)
                _currentRenderTimeUs.value = newTimeUs

                if (newTimeUs >= durationUs) {
                    pause()
                    _currentRenderTimeUs.value = 0L // loop back
                }

                delay(16) // ~60fps
            }
        }
    }

    fun pause() {
        isPlaying = false
        playbackJob?.cancel()
        playbackJob = null
    }

    fun seekTo(timeUs: Long) {
        pause()
        _currentRenderTimeUs.value = timeUs.coerceIn(0L, durationUs)
    }

    fun setDuration(durUs: Long) {
        durationUs = durUs
    }

    fun setSpeed(speed: Float) {
        speedMultiplier = speed
    }

    fun isPlaying(): Boolean = isPlaying

    fun releaseAll() {
        pause()
        audioDecoders.values.forEach { it.release() }
        audioDecoders.clear()
    }
}
