package com.changecut.core.ai

import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiSmartTrim @Inject constructor() {

    data class TrimSegment(
        val startUs: Long,
        val endUs: Long,
        val reason: String = ""
    )

    fun detectSilence(videoPath: String, thresholdDb: Double = -50.0, minSilenceMs: Long = 500): List<TrimSegment> {
        val extractor = try {
            MediaExtractor().also { it.setDataSource(videoPath) }
        } catch (e: Exception) { return emptyList() }

        val audioIdx = (0 until extractor.trackCount).firstOrNull { i ->
            extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        }
        if (audioIdx == null) { extractor.release(); return emptyList() }

        extractor.selectTrack(audioIdx)
        val format = extractor.getTrackFormat(audioIdx)
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val buffer = ByteBuffer.allocate(8192)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        val segments = mutableListOf<TrimSegment>()
        var silentStartUs = -1L
        var positionUs = extractor.sampleTime

        while (positionUs >= 0) {
            buffer.clear()
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) break

            buffer.rewind()
            val samples = sampleSize / 2
            var sumSquares = 0.0
            for (i in 0 until samples.coerceAtMost(buffer.capacity() / 2)) {
                val sample = buffer.getShort(i * 2).toDouble()
                sumSquares += sample * sample
            }
            val rms = kotlin.math.sqrt(sumSquares / samples.coerceAtLeast(1))
            val db = 20 * kotlin.math.log10(rms.coerceAtLeast(1e-10))

            if (db < thresholdDb) {
                if (silentStartUs < 0) silentStartUs = positionUs
            } else {
                if (silentStartUs >= 0) {
                    val silenceDuration = positionUs - silentStartUs
                    if (silenceDuration > minSilenceMs * 1000L) {
                        segments.add(TrimSegment(silentStartUs, positionUs, "Silence"))
                    }
                    silentStartUs = -1
                }
            }
            positionUs = extractor.sampleTime
            extractor.advance()
        }

        extractor.release()
        return segments
    }
}
