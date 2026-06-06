package com.changecut.core.ffmpeg

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FfmpegExecutor @Inject constructor() {

    private val timeoutMs = TimeUnit.MINUTES.toMillis(5)

    suspend fun execute(command: List<String>): Result<String> {
        return executeWithProgress(command) {}
    }

    suspend fun executeWithProgress(
        command: List<String>,
        onProgress: (Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        ensureActive()

        val pb = ProcessBuilder(command)
        pb.redirectErrorStream(false)
        val process = pb.start()

        val cancellationHandler = coroutineContext[Job]?.invokeOnCompletion { cause ->
            if (cause != null) {
                process.destroyForcibly()
            }
        }

        try {
            val stderrLines = mutableListOf<String>()
            var durationMs = -1.0

            val stderrJob = async {
                process.errorStream.bufferedReader().use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val l = line!!
                        stderrLines.add(l)
                        if (durationMs < 0) {
                            Regex("Duration:\\s*(\\d{2}):(\\d{2}):(\\d{2})\\.(\\d+)").find(l)?.let { m ->
                                durationMs = m.groupValues[1].toDouble() * 3600000.0 +
                                        m.groupValues[2].toDouble() * 60000.0 +
                                        m.groupValues[3].toDouble() * 1000.0 +
                                        m.groupValues[4].padEnd(3, '0').take(3).toDouble()
                            }
                        }
                        if (durationMs > 0) {
                            Regex("time=(\\d{2}):(\\d{2}):(\\d{2})\\.(\\d+)").find(l)?.let { m ->
                                val currentMs = m.groupValues[1].toDouble() * 3600000.0 +
                                        m.groupValues[2].toDouble() * 60000.0 +
                                        m.groupValues[3].toDouble() * 1000.0 +
                                        m.groupValues[4].padEnd(3, '0').take(3).toDouble()
                                onProgress((currentMs / durationMs).toFloat().coerceIn(0f, 1f))
                            }
                        }
                    }
                }
            }

            val stdoutJob = async {
                process.inputStream.bufferedReader().readText()
            }

            val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                stderrJob.cancel()
                stdoutJob.cancel()
                return@withContext Result.failure(
                    RuntimeException("FFmpeg timed out after 5 minutes")
                )
            }

            stderrJob.await()
            val stdout = stdoutJob.await()

            if (process.exitValue() == 0) {
                onProgress(1f)
                Result.success(command.last())
            } else {
                val errorMsg = stderrLines.joinToString("\n")
                Result.failure(
                    RuntimeException(
                        errorMsg.ifBlank { stdout.ifBlank { "FFmpeg process failed" } }
                    )
                )
            }
        } finally {
            cancellationHandler?.dispose()
        }
    }

    fun buildCommand(vararg args: String): List<String> {
        return listOf("ffmpeg") + args.toList()
    }

    fun getMediaInfo(inputPath: String): MediaInfo? {
        return try {
            val output = inspectMedia(inputPath)

            var durationMs = 0L
            var width = 0
            var height = 0
            var rotation = 0

            for (line in output.lines()) {
                Regex("Duration:\\s*(\\d{2}):(\\d{2}):(\\d{2})\\.(\\d+)").find(line)?.let { m ->
                    durationMs = m.groupValues[1].toLong() * 3600000L +
                            m.groupValues[2].toLong() * 60000L +
                            m.groupValues[3].toLong() * 1000L +
                            m.groupValues[4].padEnd(3, '0').take(3).toLong()
                }
                if (line.contains("Video:")) {
                    Regex("(\\d+)x(\\d+)").find(line)?.let { m ->
                        width = m.groupValues[1].toInt()
                        height = m.groupValues[2].toInt()
                    }
                }
                Regex("rotate\\s*:\\s*(\\d+)").find(line)?.let { m ->
                    rotation = m.groupValues[1].toInt()
                }
                Regex("rotate=(\\d+)").find(line)?.let { m ->
                    rotation = m.groupValues[1].toInt()
                }
            }

            if (durationMs == 0L && width == 0) return null
            MediaInfo(durationMs, width, height, rotation)
        } catch (_: Exception) {
            null
        }
    }

    fun hasAudioStream(inputPath: String): Boolean {
        return runCatching {
            inspectMedia(inputPath).lines().any { line -> line.contains("Audio:") }
        }.getOrDefault(false)
    }

    private fun inspectMedia(inputPath: String): String {
        val pb = ProcessBuilder("ffmpeg", "-i", inputPath)
        pb.redirectErrorStream(true)
        val process = pb.start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor(5, TimeUnit.SECONDS)
        return output
    }
}
