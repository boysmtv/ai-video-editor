package com.changecut.core.ai

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiVoiceIsolation @Inject constructor() {

    fun buildNoiseGateFilter(threshold: Float = -50f, attack: Float = 0.01f, release: Float = 0.1f): String {
        return "silenceremove=start_periods=1:start_duration=0.5:start_threshold=${threshold}dB," +
                "afftdn=nr=12:nf=-25:nt=w"
    }

    fun buildSpectralSubtractionFilter(noiseReduction: Float = 0.8f): String {
        return "afftdn=nr=${(noiseReduction * 15).toInt()}:nf=-${(noiseReduction * 30).toInt()}"
    }

    fun buildFullVoiceIsolation(): String {
        return "highpass=f=200,lowpass=f=3000,afftdn=nr=15:nf=-30"
    }
}
