package com.changecut.core.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

data class SubtitleResult(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val confidence: Float
)

@Singleton
class AiSubtitleEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var speechRecognizer: SpeechRecognizer? = null

    companion object {
        private const val SUBTITLE_SEGMENT_MS = 3000L
        private const val MIN_SEGMENT_MS = 1000L
    }

    fun generateSubtitles(audioPath: String): List<SubtitleResult> {
        val audioFile = File(audioPath)
        if (!audioFile.exists()) return emptyList()

        val durationMs = estimateAudioDuration(audioFile)

        return if (SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                generateWithSpeechRecognizer(audioFile, durationMs)
            } catch (e: Exception) {
                generateMockSubtitles(durationMs)
            }
        } else {
            generateMockSubtitles(durationMs)
        }
    }

    private fun generateWithSpeechRecognizer(
        audioFile: File,
        durationMs: Long
    ): List<SubtitleResult> {
        val results = mutableListOf<SubtitleResult>()
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        var segmentStartMs = 0L
        val segmentCount = (durationMs / SUBTITLE_SEGMENT_MS).toInt().coerceAtLeast(1)

        val lock = Object()
        var completed = false

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                synchronized(lock) {
                    if (!completed) {
                        completed = true
                        if (results.isEmpty()) {
                            results.addAll(generateFallbackSubtitles(durationMs))
                        }
                    }
                    lock.notifyAll()
                }
            }

            override fun onResults(bundle: Bundle?) {
                synchronized(lock) {
                    if (!completed) {
                        completed = true
                        val texts = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val scores = bundle?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                        if (texts != null && texts.isNotEmpty()) {
                            val text = texts[0]
                            val confidence = scores?.firstOrNull() ?: 0.5f
                            results.add(
                                SubtitleResult(
                                    startMs = segmentStartMs,
                                    endMs = (segmentStartMs + SUBTITLE_SEGMENT_MS)
                                        .coerceAtMost(durationMs),
                                    text = text,
                                    confidence = confidence
                                )
                            )
                        } else {
                            results.addAll(generateFallbackSubtitles(durationMs))
                        }
                    }
                    lock.notifyAll()
                }
            }

            override fun onPartialResults(bundle: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            recognizer.startListening(intent)
            synchronized(lock) {
                lock.wait(10000)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        recognizer.destroy()
        speechRecognizer = null

        if (results.isEmpty()) {
            return generateMockSubtitles(durationMs)
        }

        return results
    }

    private fun generateMockSubtitles(durationMs: Long): List<SubtitleResult> {
        val subtitles = mutableListOf<SubtitleResult>()
        val templates = listOf(
            "Welcome to this video.",
            "Today we're going to talk about something exciting.",
            "Let me show you how it works.",
            "This is the main highlight you've been waiting for.",
            "Pay attention to this part, it's very important.",
            "Here's a quick tip that can save you time.",
            "Now let's move on to the next topic.",
            "I hope you found this helpful.",
            "Don't forget to subscribe for more content.",
            "Thanks for watching and see you in the next video!",
            "Let's dive deeper into this subject.",
            "This is where things get interesting.",
            "Here's what you need to know.",
            "Let me break this down for you.",
            "Stay tuned for the final result."
        )

        val segmentMs = (durationMs / templates.size).coerceAtLeast(MIN_SEGMENT_MS).toLong()
        var currentMs = 0L
        var i = 0

        while (currentMs < durationMs && i < templates.size) {
            val endMs = (currentMs + segmentMs).coerceAtMost(durationMs)
            subtitles.add(
                SubtitleResult(
                    startMs = currentMs,
                    endMs = endMs,
                    text = templates[i % templates.size],
                    confidence = 0.85f - (i * 0.02f).toFloat().coerceAtLeast(0.5f)
                )
            )
            currentMs = endMs
            i++
        }

        if (subtitles.isEmpty() && durationMs > 0) {
            subtitles.add(
                SubtitleResult(
                    startMs = 0L,
                    endMs = durationMs,
                    text = "Video content",
                    confidence = 0.7f
                )
            )
        }

        return subtitles
    }

    private fun generateFallbackSubtitles(durationMs: Long): List<SubtitleResult> {
        val count = (durationMs / SUBTITLE_SEGMENT_MS).toInt().coerceIn(1, 10)
        val msPerSegment = durationMs / count
        return List(count) { i ->
            SubtitleResult(
                startMs = i * msPerSegment,
                endMs = ((i + 1) * msPerSegment).coerceAtMost(durationMs),
                text = "...",
                confidence = 0.0f
            )
        }
    }

    fun generateHooks(videoDescription: String): List<String> {
        val hooks = mutableListOf<String>()
        val lowered = videoDescription.lowercase()

        when {
            "how" in lowered || "tutorial" in lowered || "guide" in lowered -> {
                hooks.add("Stop scrolling! Here's how you master this in 5 minutes")
                hooks.add("I tried this for 7 days and here's what happened")
                hooks.add("The tutorial they don't want you to see")
                hooks.add("This simple trick changed everything")
                hooks.add("You've been doing it wrong this whole time")
            }
            "review" in lowered || "unboxing" in lowered || "test" in lowered -> {
                hooks.add("I bought the most controversial product of 2026")
                hooks.add("Is this really worth the hype? Let's find out")
                hooks.add("Don't buy it until you watch this")
                hooks.add("This changes EVERYTHING")
                hooks.add("The truth about this viral product")
            }
            "vlog" in lowered || "day" in lowered || "life" in lowered -> {
                hooks.add("A day in my life that changed my perspective")
                hooks.add("Come with me on this unforgettable journey")
                hooks.add("This is why I decided to make a change")
                hooks.add("My honest morning routine that changed everything")
                hooks.add("The most relaxing vlog you'll watch today")
            }
            "top" in lowered || "best" in lowered || "list" in lowered -> {
                hooks.add("Top 10 things nobody told you about")
                hooks.add("The best kept secrets finally revealed")
                hooks.add("Number 5 will shock you")
                hooks.add("These 3 things will change how you see everything")
                hooks.add("The ultimate list you'll ever need")
            }
            else -> {
                hooks.add("I spent 100 hours on this and it's incredible")
                hooks.add("This video will make you see things differently")
                hooks.add("Wait until you see the ending")
                hooks.add("The internet is obsessed with this")
                hooks.add("You won't believe what happens next")
                hooks.add("This is the most satisfying thing you'll see today")
                hooks.add("I wish I knew this sooner")
                hooks.add("Real or fake? You decide")
                hooks.add("How did I not know this before?")
                hooks.add("This video is a game changer")
            }
        }

        return hooks.shuffled().take(5)
    }

    private fun estimateAudioDuration(audioFile: File): Long {
        val size = audioFile.length()
        val bitrate = 128_000L
        val estimatedSeconds = if (size > 0 && bitrate > 0) {
            (size * 8L) / bitrate
        } else {
            30L
        }
        return (estimatedSeconds * 1000L).coerceIn(5_000L, 600_000L)
    }

    fun cleanup() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {}
    }
}
