package com.changecut.core.ml

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class SpeechTranscriptEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class TranscriptWord(
        val word: String,
        val startMs: Long,
        val endMs: Long,
        val confidence: Float
    )

    data class TranscriptSegment(
        val text: String,
        val startMs: Long,
        val endMs: Long,
        val words: List<TranscriptWord> = emptyList()
    )

    private val _segments = MutableStateFlow<List<TranscriptSegment>>(emptyList())
    val segments: StateFlow<List<TranscriptSegment>> = _segments.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private var isProcessing = false

    fun transcribe(audioPath: String) {
        if (isProcessing) return
        isProcessing = true
        _segments.value = emptyList()
        _progress.value = 0f

        try {
            val segments = detectSilenceSegments(audioPath)
            _segments.value = segments
        } catch (e: Exception) {
            _segments.value = emptyList()
        }
        _progress.value = 1f
        isProcessing = false
    }

    private fun detectSilenceSegments(audioPath: String): List<TranscriptSegment> {
        val segments = mutableListOf<TranscriptSegment>()
        try {
            val pcmData = decodeAudioToPcmShortArray(audioPath) ?: return segments
            val sampleRate = 44100
            val windowSize = 2048
            val hopSize = 512
            val silenceThreshold = 0.02f
            val minSilenceMs = 300L

            val rmsValues = mutableListOf<Float>()
            var pos = 0
            while (pos + windowSize <= pcmData.size) {
                var sumSq = 0f
                for (i in 0 until windowSize) {
                    val sample = pcmData[pos + i] / 32768f
                    sumSq += sample * sample
                }
                sumSq = kotlin.math.sqrt(sumSq / windowSize)
                rmsValues.add(sumSq)
                pos += hopSize
            }

            val isSilent = rmsValues.map { it < silenceThreshold }
            val hopMs = hopSize * 1000 / sampleRate.toLong()
            var segmentStart = 0L
            var inSilence = true

            for (i in isSilent.indices) {
                val timeMs = i * hopMs
                if (isSilent[i] && !inSilence && (timeMs - segmentStart) > minSilenceMs) {
                    segments.add(TranscriptSegment(
                        text = "[speech]",
                        startMs = segmentStart,
                        endMs = timeMs
                    ))
                    segmentStart = timeMs
                    inSilence = true
                } else if (!isSilent[i]) {
                    inSilence = false
                }
            }

            if (!inSilence) {
                segments.add(TranscriptSegment(
                    text = "[speech]",
                    startMs = segmentStart,
                    endMs = isSilent.size * hopMs
                ))
            }

            if (segments.isEmpty()) {
                segments.add(TranscriptSegment(
                    text = "[audio]",
                    startMs = 0L,
                    endMs = pcmData.size * 1000L / sampleRate
                ))
            }
        } catch (_: Exception) { }
        return segments
    }

    private fun decodeAudioToPcmShortArray(audioPath: String): ShortArray? {
        var extractor: MediaExtractor? = null
        var codec: MediaCodec? = null
        return try {
            extractor = MediaExtractor()
            extractor.setDataSource(audioPath)
            val trackIndex = selectAudioTrack(extractor) ?: return null
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val allPcm = mutableListOf<Short>()
            val bufferInfo = MediaCodec.BufferInfo()
            var isEos = false
            val decodeTimeout = 10000L

            while (!isEos) {
                val inputIndex = codec.dequeueInputBuffer(decodeTimeout)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex) ?: break
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, decodeTimeout)
                if (outputIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputIndex) ?: continue
                    val shortBuffer = outputBuffer.order(ByteOrder.nativeOrder()).asShortBuffer()
                    val shorts = ShortArray(shortBuffer.remaining())
                    shortBuffer.get(shorts)
                    allPcm.addAll(shorts.toList())
                    codec.releaseOutputBuffer(outputIndex, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        isEos = true
                    }
                }
            }

            ShortArray(allPcm.size) { allPcm[it] }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor?.release() }
        }
    }

    private fun selectAudioTrack(extractor: MediaExtractor): Int? {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) return i
        }
        return null
    }
}
