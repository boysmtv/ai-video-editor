package com.changecut.core.ffmpeg

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

@Singleton
class AudioExtractEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ffmpegExecutor: FfmpegExecutor
) {
    suspend fun extractAudio(
        videoPath: String,
        outputPath: String,
        format: String = "mp3"
    ): Result<String> {
        val codec = when (format.lowercase()) {
            "mp3" -> "libmp3lame"
            "aac" -> "aac"
            "wav" -> "pcm_s16le"
            "ogg" -> "libvorbis"
            "flac" -> "flac"
            else -> "libmp3lame"
        }
        return execute(
            "-i", videoPath,
            "-vn",
            "-acodec", codec,
            "-y", outputPath
        ).map { outputPath }
    }

    suspend fun getAudioWaveform(audioPath: String): Result<List<Float>> {
        val cacheDir = File(context.cacheDir, "changecut/waveform")
        cacheDir.mkdirs()
        val pcmPath = File(cacheDir, "waveform_${audioPath.hashCode()}.pcm").absolutePath
        return execute(
            "-i", audioPath,
            "-ac", "1",
            "-ar", "22050",
            "-f", "s16le",
            "-y", pcmPath
        ).map {
            val pcmFile = File(pcmPath)
            if (!pcmFile.exists()) return@map emptyList<Float>()
            val bytes = pcmFile.readBytes()
            val samples = mutableListOf<Float>()
            var i = 0
            while (i + 1 < bytes.size) {
                val sample = ((bytes[i].toInt() and 0xFF) or (bytes[i + 1].toInt() shl 8)).toShort()
                samples.add(sample.toFloat() / Short.MAX_VALUE)
                i += 2
            }
            pcmFile.delete()
            val targetBars = 200
            if (samples.isEmpty()) return@map emptyList<Float>()
            val chunkSize = (samples.size / targetBars).coerceAtLeast(1)
            samples.chunked(chunkSize).map { chunk ->
                sqrt(chunk.sumOf { (it.toDouble()).pow(2) } / chunk.size).toFloat().coerceIn(0f, 1f)
            }
        }
    }

    suspend fun mixAudio(
        videoPath: String,
        audioPath: String,
        outputPath: String,
        volume: Float = 1.0f
    ): Result<String> {
        val vol = volume.coerceIn(0f, 2f)
        return execute(
            "-i", videoPath,
            "-i", audioPath,
            "-filter_complex",
            "[1:a]volume=${vol}[a1];[0:a][a1]amix=inputs=2:duration=first:dropout_transition=2",
            "-c:v", "copy",
            "-y", outputPath
        ).map { outputPath }
    }

    suspend fun replaceAudio(
        videoPath: String,
        audioPath: String,
        outputPath: String
    ): Result<String> = execute(
        "-i", videoPath,
        "-i", audioPath,
        "-c:v", "copy",
        "-map", "0:v:0",
        "-map", "1:a:0",
        "-shortest",
        "-y", outputPath
    ).map { outputPath }

    private suspend fun execute(vararg args: String): Result<Unit> {
        val command = ffmpegExecutor.buildCommand(*args)
        return ffmpegExecutor.execute(command).map { }
    }
}
