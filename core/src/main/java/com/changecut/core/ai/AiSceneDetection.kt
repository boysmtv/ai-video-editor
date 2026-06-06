package com.changecut.core.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

data class SceneChangeResult(
    val timeUs: Long,
    val confidence: Double,
    val thumbnail: Bitmap?
)

data class SilenceSegment(
    val startTimeUs: Long,
    val endTimeUs: Long
)

@Singleton
class AiSceneDetection @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val FRAME_INTERVAL_US = 500_000L
        private const val SCENE_CHANGE_THRESHOLD = 0.35
        private const val HISTOGRAM_BINS = 64
        private const val SILENCE_AMPLITUDE_THRESHOLD = 0.02
        private const val MIN_SILENCE_DURATION_US = 200_000L
    }

    fun detectSceneChanges(videoPath: String): List<SceneChangeResult> {
        val results = mutableListOf<SceneChangeResult>()
        val retriever = MediaMetadataRetriever()

        try {
            retriever.setDataSource(videoPath)
            val durationStr = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )
            val durationMs = durationStr?.toLongOrNull() ?: return results
            val durationUs = durationMs * 1000L

            var previousHistogram: DoubleArray? = null
            var timeUs = 0L

            while (timeUs < durationUs) {
                val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (frame != null) {
                    val histogram = computeHistogram(frame)
                    if (previousHistogram != null) {
                        val diff = histogramDifference(previousHistogram, histogram)
                        if (diff > SCENE_CHANGE_THRESHOLD) {
                            results.add(
                                SceneChangeResult(
                                    timeUs = timeUs,
                                    confidence = diff.coerceIn(0.0, 1.0),
                                    thumbnail = frame
                                )
                            )
                        }
                    }
                    previousHistogram = histogram
                }
                timeUs += FRAME_INTERVAL_US
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        return results
    }

    fun detectSilence(
        audioPath: String,
        threshold: Double = SILENCE_AMPLITUDE_THRESHOLD
    ): List<SilenceSegment> {
        val segments = mutableListOf<SilenceSegment>()
        val file = File(audioPath)
        if (!file.exists()) return segments

        try {
            val samples = readPcmSamples(file)
            if (samples.isEmpty()) return segments

            val sampleRate = 44100
            val samplesPerMs = sampleRate / 1000
            val windowSize = samplesPerMs * 50
            var silenceStart: Long? = null

            var i = 0
            while (i + windowSize <= samples.size) {
                val window = samples.sliceArray(i until i + windowSize)
                val rms = sqrt(window.sumOf { s -> (s.toDouble() * s.toDouble()) } / windowSize)
                val isSilent = rms < threshold

                if (isSilent && silenceStart == null) {
                    silenceStart = (i.toLong() * 1_000_000L) / sampleRate
                } else if (!isSilent && silenceStart != null) {
                    val endTime = (i.toLong() * 1_000_000L) / sampleRate
                    if (endTime - silenceStart!! >= MIN_SILENCE_DURATION_US) {
                        segments.add(SilenceSegment(silenceStart!!, endTime))
                    }
                    silenceStart = null
                }

                i += windowSize / 2
            }

            if (silenceStart != null) {
                segments.add(
                    SilenceSegment(
                        silenceStart,
                        (samples.size.toLong() * 1_000_000L) / sampleRate
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return segments
    }

    private fun computeHistogram(bitmap: Bitmap): DoubleArray {
        val histogram = DoubleArray(HISTOGRAM_BINS * 3)
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val total = pixels.size.toDouble()

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val rBin = (r * HISTOGRAM_BINS / 256).coerceIn(0, HISTOGRAM_BINS - 1)
            val gBin = (g * HISTOGRAM_BINS / 256).coerceIn(0, HISTOGRAM_BINS - 1)
            val bBin = (b * HISTOGRAM_BINS / 256).coerceIn(0, HISTOGRAM_BINS - 1)
            histogram[rBin]++
            histogram[HISTOGRAM_BINS + gBin]++
            histogram[2 * HISTOGRAM_BINS + bBin]++
        }

        for (i in histogram.indices) {
            histogram[i] /= total
        }

        return histogram
    }

    private fun histogramDifference(h1: DoubleArray, h2: DoubleArray): Double {
        if (h1.size != h2.size) return 1.0
        var diff = 0.0
        for (i in h1.indices) {
            diff += abs(h1[i] - h2[i])
        }
        return diff / 2.0
    }

    private fun readPcmSamples(file: File): ShortArray {
        return try {
            val data = file.readBytes()
            val samples = ShortArray(data.size / 2)
            for (i in samples.indices) {
                val lo = data[i * 2].toInt() and 0xFF
                val hi = data[i * 2 + 1].toInt() shl 8
                samples[i] = (hi or lo).toShort()
            }
            samples
        } catch (e: Exception) {
            ShortArray(0)
        }
    }
}
