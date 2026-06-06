package com.changecut.core.ffmpeg

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChromaKeyEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ffmpegExecutor: FfmpegExecutor
) {
    companion object {
        const val DEFAULT_GREEN_HEX = "0x00FF00"
        const val DEFAULT_BLUE_HEX = "0x0000FF"
        const val DEFAULT_SIMILARITY = 0.2
        const val DEFAULT_BLEND = 0.1
        const val BLUE_SIMILARITY = 0.3
    }

    suspend fun removeBackground(
        videoPath: String,
        colorHex: String = DEFAULT_GREEN_HEX,
        similarity: Double = DEFAULT_SIMILARITY,
        blend: Double = DEFAULT_BLEND,
        outputPath: String
    ): Result<String> {
        val filter = "colorkey=color=$colorHex:similarity=$similarity:blend=$blend"
        return execute(
            "-i", videoPath,
            "-vf", filter,
            "-c:a", "copy",
            "-y", outputPath
        ).map { outputPath }
    }

    suspend fun removeBackgroundWithReplace(
        videoPath: String,
        bgVideoPath: String,
        colorHex: String = DEFAULT_GREEN_HEX,
        similarity: Double = DEFAULT_SIMILARITY,
        blend: Double = DEFAULT_BLEND,
        outputPath: String
    ): Result<String> {
        val colorkeyFilter = "colorkey=color=$colorHex:similarity=$similarity:blend=$blend"
        val filterComplex = "[1:v]scale=iw:ih[bg];[0:v]$colorkeyFilter[fg];[bg][fg]overlay=0:0"
        return execute(
            "-i", videoPath,
            "-i", bgVideoPath,
            "-filter_complex", filterComplex,
            "-c:a", "copy",
            "-y", outputPath
        ).map { outputPath }
    }

    private suspend fun execute(vararg args: String): Result<Unit> {
        val command = ffmpegExecutor.buildCommand(*args)
        return ffmpegExecutor.execute(command).map { }
    }
}
