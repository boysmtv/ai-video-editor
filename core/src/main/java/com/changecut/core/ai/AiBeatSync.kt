package com.changecut.core.ai

import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiBeatSync @Inject constructor() {

    data class BeatInfo(
        val timeUs: Long,
        val intensity: Float,
        val frequency: Float
    )

    fun detectBeats(audioPath: String, sampleDurationMs: Int = 100): List<BeatInfo> {
        val extractor = try {
            MediaExtractor().also { it.setDataSource(audioPath) }
        } catch (e: Exception) { return emptyList() }

        val trackIdx = (0 until extractor.trackCount).firstOrNull { i ->
            extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        }
        if (trackIdx == null) { extractor.release(); return emptyList() }

        extractor.selectTrack(trackIdx)
        val format = extractor.getTrackFormat(trackIdx)
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val buffer = ByteBuffer.allocate(8192)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        val beats = mutableListOf<BeatInfo>()
        var lastEnergy = 0.0
        var timeUs = 0L

        while (true) {
            buffer.clear()
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) break

            buffer.rewind()
            val samples = sampleSize / 2
            var energy = 0.0
            for (i in 0 until samples.coerceAtMost(buffer.capacity() / 2)) {
                energy += kotlin.math.abs(buffer.getShort(i * 2).toDouble())
            }
            energy /= samples.coerceAtLeast(1)
            val instantEnergy = energy - lastEnergy

            if (instantEnergy > lastEnergy * 1.5 && lastEnergy > 100) {
                beats.add(BeatInfo(timeUs, instantEnergy.toFloat(), sampleRate.toFloat()))
            }
            lastEnergy = energy
            timeUs += (sampleSize / (sampleRate * channelCount * 2L)) * 1_000_000L
            extractor.advance()
        }

        extractor.release()
        return beats
    }
}
