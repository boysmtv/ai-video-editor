package com.changecut.core.ffmpeg

import com.changecut.core.editor.AudioEQDef
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioEQEngine @Inject constructor() {

    fun buildEQFilter(eq: AudioEQDef): String {
        val parts = mutableListOf<String>()
        if (eq.lowGain != 0f) {
            parts.add("equalizer=f=${eq.lowFreq}:t=h:w=200:g=${eq.lowGain}")
        }
        if (eq.midGain != 0f) {
            parts.add("equalizer=f=${eq.midFreq}:t=pe:w=500:g=${eq.midGain}")
        }
        if (eq.highGain != 0f) {
            parts.add("equalizer=f=${eq.highFreq}:t=h:w=2000:g=${eq.highGain}")
        }
        return parts.joinToString(",")
    }

    fun buildCompressorFilter(threshold: Float, ratio: Float): String {
        if (ratio <= 1f) return ""
        return "compand=attacks=0.1:decays=0.1:points=-80/-80|${threshold}/${threshold}|-20/-${(threshold / ratio).toInt()}|0/0"
    }

    fun buildDuckingFilter(duckAmount: Float, voiceTrackIndex: Int = 1, musicTrackIndex: Int = 0): String {
        if (duckAmount <= 0f) return ""
        val reduction = (1 - duckAmount.coerceIn(0f, 1f)) * 100
        return "[${voiceTrackIndex}:a]asplit[voice][voice2];" +
                "[${musicTrackIndex}:a]asplit[music][music2];" +
                "[voice]dynaudnorm[voice_norm];" +
                "[voice_norm]silencedetect=n=-50dB:d=0.5[DET];" +
                "[music2]volume=1.0[music_orig];" +
                "[music_orig]volume=${(1 - duckAmount * 0.5f)}:enable='between(t,0,99999)'[music_ducked];" +
                "[music_ducked][voice_norm]amix=inputs=2:duration=first"
    }

    fun buildFullAudioProcess(eq: AudioEQDef): String {
        val parts = mutableListOf<String>()
        val eqFilter = buildEQFilter(eq)
        if (eqFilter.isNotEmpty()) parts.add(eqFilter)
        val compFilter = buildCompressorFilter(eq.compressorThreshold, eq.compressorRatio)
        if (compFilter.isNotEmpty()) parts.add(compFilter)
        return parts.joinToString(",")
    }
}
