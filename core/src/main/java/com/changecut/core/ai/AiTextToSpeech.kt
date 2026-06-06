package com.changecut.core.ai

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.suspendCancellableCoroutine

enum class VoiceType(val displayName: String, val locale: Locale?) {
    MALE("Male", Locale.US),
    FEMALE("Female", Locale.UK),
    NEUTRAL("Neutral", Locale.getDefault()),
    NARRATOR("Narrator", Locale.CANADA),
    CUSTOM("Custom", null)
}

@Singleton
class AiTextToSpeech @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var selectedVoice: VoiceType = VoiceType.NEUTRAL

    private val initLock = Object()

    init {
        initialize()
    }

    private fun initialize() {
        tts = TextToSpeech(context) { status ->
            synchronized(initLock) {
                isInitialized = (status == TextToSpeech.SUCCESS)
                initLock.notifyAll()
            }
        }
    }

    suspend fun generateVoiceOver(
        text: String,
        voiceType: VoiceType,
        speed: Float = 1.0f,
        outputPath: String? = null
    ): Result<String> {
        if (!isInitialized) {
            synchronized(initLock) {
                if (!isInitialized) {
                    try {
                        initLock.wait(3000)
                    } catch (_: InterruptedException) {}
                }
            }
        }

        val engine = tts ?: return Result.failure(Exception("TTS engine not initialized"))

        return try {
            synchronized(engine) {
                val filePath = outputPath ?: File(
                    context.cacheDir,
                    "voiceover/voice_${UUID.randomUUID()}.wav"
                ).absolutePath
                File(filePath).parentFile?.mkdirs()

                engine.language = voiceType.locale ?: Locale.getDefault()
                engine.setSpeechRate(speed.coerceIn(0.5f, 2.0f))
                engine.setPitch(if (voiceType == VoiceType.FEMALE) 1.2f else if (voiceType == VoiceType.MALE) 0.85f else 1.0f)

                selectedVoice = voiceType

                applyVoice(engine, voiceType)

                val file = File(filePath)
                val result = engine.synthesizeToFile(text, null, file, "voiceover_utterance")

                if (result != TextToSpeech.SUCCESS) {
                    return Result.failure(Exception("Failed to start TTS synthesis"))
                }

                Result.success(filePath)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun applyVoice(engine: TextToSpeech, voiceType: VoiceType) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            try {
                val voices = engine.voices ?: return
                val matchingVoice = when (voiceType) {
                    VoiceType.MALE -> voices.find { it.name.contains("male", true) }
                    VoiceType.FEMALE -> voices.find { it.name.contains("female", true) }
                    VoiceType.NARRATOR -> voices.find { it.name.contains("narrator", true) || it.name.contains("english", true) }
                    else -> null
                }
                matchingVoice?.let { engine.voice = it }
            } catch (_: Exception) {}
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            engine.voice?.let { v ->
                if (v.features?.contains(TextToSpeech.Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS) == true) {
                    engine.voice?.let { engine.voice = it }
                }
            }
        }
    }

    fun getAvailableVoices(): List<VoiceType> {
        val available = mutableListOf<VoiceType>()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            val voices = tts?.voices ?: emptyList()
            val hasMale = voices.any { it.name.contains("male", true) }
            val hasFemale = voices.any { it.name.contains("female", true) }
            val hasNarrator = voices.any { it.name.contains("narrator", true) }
            val hasNeutral = voices.any {
                !it.name.contains("male", true) &&
                        !it.name.contains("female", true) &&
                        !it.name.contains("narrator", true)
            }

            if (hasMale) available.add(VoiceType.MALE)
            if (hasFemale) available.add(VoiceType.FEMALE)
            if (hasNarrator) available.add(VoiceType.NARRATOR)
            if (hasNeutral) available.add(VoiceType.NEUTRAL)
        }

        if (available.isEmpty()) {
            available.addAll(VoiceType.entries.filter { it != VoiceType.CUSTOM })
        }

        return available
    }

    fun cleanup() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (_: Exception) {}
    }
}
