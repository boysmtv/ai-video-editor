package com.changecut.core.ffmpeg

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class SpeedRampPoint(
    val timeUs: Long,
    val speed: Float
)

@Singleton
class SpeedRampEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ffmpegExecutor: FfmpegExecutor
) {
    private val cacheDir: File get() = File(context.cacheDir, "changecut/speed_ramp")

    suspend fun applySpeed(
        videoPath: String,
        speed: Float,
        outputPath: String
    ): Result<String> {
        val pts = 1.0 / speed.coerceAtLeast(0.01f)
        val filter = "setpts=${pts}*PTS"
        val audioFilter = if (speed != 0f) "atempo=${speed.coerceIn(0.5f, 2.0f)}" else "atempo=1.0"
        return execute(
            "-i", videoPath,
            "-vf", filter,
            "-af", audioFilter,
            "-y", outputPath
        ).map { outputPath }
    }

    suspend fun applySpeedRamp(
        videoPath: String,
        keyframes: List<SpeedRampPoint>,
        outputPath: String
    ): Result<String> {
        if (keyframes.size < 2) {
            return applySpeed(videoPath, keyframes.firstOrNull()?.speed ?: 1f, outputPath)
        }

        cacheDir.mkdirs()
        val segmentPaths = mutableListOf<String>()
        val sorted = keyframes.sortedBy { it.timeUs }

        return try {
            for (i in 0 until sorted.size - 1) {
                val start = sorted[i]
                val end = sorted[i + 1]
                val segmentPath = File(cacheDir, "seg_${UUID.randomUUID()}.mp4").absolutePath
                val pts = 1.0 / end.speed.coerceAtLeast(0.01f)
                val atempo = end.speed.coerceIn(0.5f, 2.0f)

                val trimResult = execute(
                    "-i", videoPath,
                    "-ss", formatTimeUs(start.timeUs),
                    "-to", formatTimeUs(end.timeUs),
                    "-vf", "setpts=$pts*PTS",
                    "-af", "atempo=$atempo",
                    "-y", segmentPath
                )
                if (trimResult.isFailure) {
                    return trimResult.map { outputPath }
                }
                segmentPaths.add(segmentPath)
            }

            val listFile = File(cacheDir, "concat_${UUID.randomUUID()}.txt")
            listFile.writeText(segmentPaths.joinToString("\n") { "file '$it'" })

            execute(
                "-f", "concat",
                "-safe", "0",
                "-i", listFile.absolutePath,
                "-c", "copy",
                "-y", outputPath
            ).map { outputPath }
        } finally {
            segmentPaths.forEach { File(it).delete() }
            File(cacheDir, listOfNotNull(cacheDir.listFiles()?.firstOrNull {
                it.name.startsWith("concat_")
            }?.name).firstOrNull() ?: "").delete()
        }
    }

    fun getSpeedRampPoints(totalDurationUs: Long): List<SpeedRampPoint> {
        val third = totalDurationUs / 3
        return listOf(
            SpeedRampPoint(timeUs = 0L, speed = 1.0f),
            SpeedRampPoint(timeUs = third, speed = 2.0f),
            SpeedRampPoint(timeUs = third * 2, speed = 0.5f),
            SpeedRampPoint(timeUs = totalDurationUs, speed = 1.0f)
        )
    }

    private suspend fun execute(vararg args: String): Result<Unit> {
        val command = ffmpegExecutor.buildCommand(*args)
        return ffmpegExecutor.execute(command).map { }
    }

    private fun formatTimeUs(us: Long): String {
        val totalSec = us / 1_000_000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        val millis = (us % 1_000_000) / 1000
        return String.format("%02d:%02d:%02d.%03d", h, m, s, millis)
    }
}
